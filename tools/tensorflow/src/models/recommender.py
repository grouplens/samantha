
import tensorflow as tf

from src.builder import ModelBuilder


class RecommenderBuilder(ModelBuilder):

    def __init__(self,
                 user_model,
                 prediction_model,
                 page_size=3,
                 attr2config=None,
                 embedding_attrs=None,
                 target2config=None,
                 eval_metrics='MAP@1',
                 loss_split_steps=500,
                 max_train_steps=500,
                 train_steps=500,
                 eval_steps=1,
                 filter_unrecognized=False):
        self._user_model = user_model
        self._prediction_model = prediction_model
        self._page_size = page_size
        if attr2config is None:
            self._attr2config = {
                'action': {
                    'vocab_size': 10,
                    'is_numerical': False,
                    'embedding_dim': 10,
                    'level': 'item',
                }
            }
        else:
            self._attr2config = attr2config
        if embedding_attrs is None:
            self._embedding_attrs = self._attr2config.keys()
        else:
            self._embedding_attrs = embedding_attrs
        if target2config is None:
            self._target2config = {
                'action': {
                    'weight': 1.0,
                }
            }
        else:
            self._target2config = target2config
        self._eval_metrics = eval_metrics
        self._loss_split_steps = loss_split_steps
        self._max_train_steps = max_train_steps
        self._train_steps = train_steps
        self._eval_steps = eval_steps
        self._filter_unrecognized = filter_unrecognized
        self._test_tensors = {}

    def test_tensors(self):
        return self._test_tensors

    def _compute_map_metrics(self, labels, logits, metric):
        K = metric.split('@')[1].split(',')
        updates = []
        for k in K:
            with tf.variable_scope('MAP_K%s' % k):
                map_value, map_update = tf.metrics.sparse_average_precision_at_k(
                    tf.cast(labels, tf.int64), logits, int(k))
                updates.append(map_update)
                tf.summary.scalar('MAP_K%s' % k, map_value)
        return updates

    def _compute_target_loss(self, user_model, indices, labels,
            paras, target, config, metrics=None, mode='train'):
        batch_idx = tf.reshape(
                tf.slice(indices,
                         begin=[0, 0],
                         size=[tf.shape(indices)[0], 1]),
                [tf.shape(indices)[0], 1])
        step_idx = tf.slice(indices,
                            begin=[0, 1],
                            size=[tf.shape(indices)[0], 1])
        shifted_step_idx = tf.reshape(step_idx - 1, [tf.shape(indices)[0], 1])
        used_indices = tf.concat([batch_idx, shifted_step_idx], 1)
        used_model = tf.gather_nd(user_model, used_indices)
        used_labels = tf.gather_nd(labels, indices)
        metric_updates = []
        if self._loss_split_steps < self._max_train_steps and metrics is None:
            mask_idx = tf.reshape(step_idx, [-1])
            loss = 0
            with tf.variable_scope('mask'):
                for i in range(0, self._max_train_steps, self._loss_split_steps):
                    loss_mask = tf.logical_and(mask_idx >= i, mask_idx < i + self._loss_split_steps)
                    masked_labels = tf.boolean_mask(used_labels, loss_mask)
                    masked_output = tf.boolean_mask(used_model, loss_mask)
                    _, masked_loss, _ = self._prediction_model.get_target_prediction_loss(
                        masked_output, masked_labels, paras, target, config, mode)
                    loss += masked_loss
        else:
            predictions, loss, model_updates = self._prediction_model.get_target_prediction_loss(
                used_model, used_labels, paras, target, config, mode)
            metric_updates += model_updates
            if metrics is not None:
                for metric in metrics.split(' '):
                    if 'MAP' in metric:
                        metric_updates += self._compute_map_metrics(used_labels, predictions, metric)
        return loss, metric_updates

    def _get_loss(self, sequence_length, user_model, target2label):
        length_limit = tf.minimum(
            tf.reshape(sequence_length, [-1]),
            self._max_train_steps)
        tf.summary.histogram('batch_sequence_length', length_limit)
        split_limit = tf.maximum(length_limit - self._eval_steps, 2)
        start_limit = tf.maximum(split_limit - self._train_steps - 1, 0)
        train_loss = 0.0
        eval_loss = 0.0
        num_train_labels = 0.0
        num_eval_labels = 0.0
        updates = []
        target2paras = {}
        for target, config in self._target2config.iteritems():
            non_zeros_indices = tf.cast(tf.where(target2label[target] > 0), tf.int32)
            batch_idx = tf.reshape(tf.slice(
                non_zeros_indices,
                begin=[0, 0],
                size=[tf.shape(non_zeros_indices)[0], 1]), [-1])
            step_idx = tf.reshape(tf.slice(
                non_zeros_indices,
                begin=[0, 1],
                size=[tf.shape(non_zeros_indices)[0], 1]), [-1])
            step_split_limit = tf.gather(split_limit, batch_idx)
            step_length_limit = tf.gather(length_limit, batch_idx)
            step_start_limit = tf.gather(start_limit, batch_idx)
            train_indices = tf.boolean_mask(
                non_zeros_indices, tf.logical_and(
                    step_idx > step_start_limit, step_idx < step_split_limit))
            eval_indices = tf.boolean_mask(
                non_zeros_indices, tf.logical_and(
                    step_idx >= step_split_limit, step_idx < step_length_limit))
            num_target_train_labels = tf.shape(train_indices)[0]
            num_target_eval_labels = tf.shape(eval_indices)[0]
            num_train_labels += config['weight'] * tf.cast(num_target_train_labels, tf.float32)
            num_eval_labels += config['weight'] * tf.cast(num_target_eval_labels, tf.float32)
            with tf.variable_scope(target):
                tf.summary.scalar('num_train_labels', num_target_train_labels)
                tf.summary.scalar('num_eval_labels', num_target_eval_labels)
                target2paras[target] = self._prediction_model.get_target_paras(target, config)
                train_target_loss, _ = self._compute_target_loss(
                    user_model, train_indices, target2label[target], target2paras[target],
                    target, config)
                eval_target_loss, metric_updates = self._compute_target_loss(
                    user_model, eval_indices, target2label[target], target2paras[target],
                    target, config, metrics=self._eval_metrics, mode='eval')
            train_loss += config['weight'] * train_target_loss
            eval_loss += config['weight'] * eval_target_loss
            updates += metric_updates
        train_loss = train_loss / tf.maximum(1.0, num_train_labels)
        eval_loss = eval_loss / tf.maximum(1.0, num_eval_labels)
        tf.summary.scalar('train_loss', train_loss)
        tf.summary.scalar('eval_loss', eval_loss)
        return train_loss, updates, target2paras

    def _get_inputs(self):
        attr2input = {}
        max_seq_len = None
        for attr, config in self._attr2config.iteritems():
            size = None
            if config['level'] == 'user':
                size = 1
            inputs = tf.placeholder(
                tf.int32, shape=(None, size), name='%s_idx' % attr)
            if config['level'] == 'item':
                max_seq_len = tf.shape(inputs)[1] / self._page_size
            if self._filter_unrecognized:
                inputs = inputs * tf.cast(inputs < config['vocab_size'], tf.int32)
            attr2input[attr] = inputs
        if max_seq_len is None:
            raise Exception('There must be an item level attribute in attr2config.')
        sequence_length = tf.placeholder(
                tf.int32, shape=(None, 1), name='sequence_length_val') / self._page_size
        return max_seq_len, sequence_length, attr2input

    def _get_embedders(self):
        attr2embedder = {}
        for attr in self._embedding_attrs:
            config = self._attr2config[attr]
            attr2embedder[attr] = tf.keras.layers.Embedding(
                    config['vocab_size'], config['embedding_dim'], dtype=tf.float32)
        return attr2embedder

    def _get_embeddings(self, max_seq_len, attr2input, attr2embedder):
        attr2embedding = {}
        for attr in self._embedding_attrs:
            config = self._attr2config[attr]
            inputs = attr2input[attr]
            if config['level'] == 'item':
                inputs = tf.reshape(inputs, [tf.shape(inputs)[0], max_seq_len, self._page_size])
                attr2input[attr] = inputs
                embedding = attr2embedder[attr](inputs)
                embedding = tf.reshape(embedding,
                    [
                        tf.shape(embedding)[0],
                        tf.shape(embedding)[1],
                        self._page_size * config['embedding_dim']
                    ]
                )
            else:
                embedding = attr2embedder[attr](inputs)
            attr2embedding[attr] = embedding
        return attr2embedding

    def _get_prediction(self, sequence_length, user_model, target2paras):
        seq_idx = sequence_length - 1
        batch_idx = tf.expand_dims(tf.range(tf.shape(sequence_length)[0]), 1)
        output_idx = tf.concat([batch_idx, seq_idx], 1)
        model_output = tf.gather_nd(user_model, output_idx)
        target2preds = {}
        for target, config in self._target2config.iteritems():
            target2preds[target] = self._prediction_model.get_target_prediction(
                model_output, target2paras[target], target, config)
        return target2preds

    def build_model(self):
        max_seq_len, sequence_length, attr2input = self._get_inputs()
        with tf.variable_scope('embeddings'):
            attr2embedder = self._get_embedders()
            attr2embedding = self._get_embeddings(max_seq_len, attr2input, attr2embedder)
        with tf.variable_scope('user_model'):
            user_model = self._user_model.get_user_model(
                max_seq_len, sequence_length, attr2embedding, self._attr2config)
        with tf.variable_scope('loss'):
            loss, updates, target2paras = self._get_loss(
                sequence_length, user_model, attr2input)
        with tf.variable_scope('prediction'):
            self._get_prediction(sequence_length, user_model, target2paras)
        return loss, updates

