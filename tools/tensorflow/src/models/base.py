
import tensorflow as tf

from src.builder import ModelBuilder

class BaseSequenceModelBuilder(ModelBuilder):

    def __init__(self,
                 page_size=3,
                 attr2config=None,
                 target2config=None,
                 eval_metrics='MAP@1',
                 loss_split_steps=500,
                 max_train_steps=500,
                 train_steps=500,
                 eval_steps=1,
                 filter_unrecognized=False):
        self._page_size = page_size
        # this configures each input
        #   vocab_size
        #   is_numerical
        #   embedding_dim
        #   level: user|item
        if attr2config is None:
            self._attr2config = {
                'action': {
                    'vocab_size': 10,
                    'is_numerical': False,
                    'embedding_dim': 10,
                    'level': 'item'
                }
            }
        else:
            self._attr2config = attr2config
        # this configures which input and how to predict
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

    def _step_wise_relu(self, inputs):
        relu_layer = tf.keras.layers.Dense(self._rnn_size, activation='relu')
        return relu_layer(inputs)

    def _get_rnn_output(self, inputs):
        rnn_layer = tf.keras.layers.GRU(self._rnn_size, return_sequences=True)
        return rnn_layer(inputs)

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

    def _compute_event_loss(self, rnn_output, indices, labels, softmax, metrics=None):
        batch_idx = tf.reshape(tf.slice(indices,
                                        begin=[0, 0],
                                        size=[tf.shape(indices)[0], 1]), [tf.shape(indices)[0], 1])
        step_idx = tf.reshape(tf.slice(indices,
                                       begin=[0, 1],
                                       size=[tf.shape(indices)[0], 1]) - 1, [tf.shape(indices)[0], 1])
        rnn_indices = tf.concat([batch_idx, step_idx], 1)
        used_output = tf.gather_nd(rnn_output, rnn_indices)
        valid_labels = tf.gather_nd(labels, indices)
        metric_update = []
        if self._loss_split_steps < self._max_train_steps and metrics == None:
            mask_idx = tf.reshape(tf.slice(indices,
                                           begin=[0, 1],
                                           size=[tf.shape(indices)[0], 1]) - 1, [-1])
            loss = 0
            with tf.variable_scope('mask'):
                for i in range(0, self._max_train_steps, self._loss_split_steps):
                    loss_mask = tf.logical_and(mask_idx >= i, mask_idx < i + self._loss_split_steps)
                    masked_labels = tf.boolean_mask(valid_labels, loss_mask)
                    masked_output = tf.boolean_mask(used_output, loss_mask)
                    masked_logits = softmax(masked_output)
                    masked_losses = tf.nn.sparse_softmax_cross_entropy_with_logits(
                        labels=masked_labels, logits=masked_logits)
                    loss += tf.reduce_sum(masked_losses)
        else:
            logits = softmax(used_output)
            losses = tf.nn.sparse_softmax_cross_entropy_with_logits(
                labels=valid_labels, logits=logits)
            loss = tf.reduce_sum(losses)
            if metrics != None:
                for metric in metrics.split(' '):
                    if 'MAP' in metric:
                        metric_update += self._compute_map_metrics(valid_labels, logits, metric)
        return loss, metric_update

    def _get_softmax_loss(self, sequence_length, rnn_output, label_by_event, softmax_by_event):
        with tf.variable_scope('softmax'):
            softmax_layer = {}
            for key in self._item_events:
                softmax_layer[key] = tf.keras.layers.Dense(self._item_vocab_size)

        length_limit = tf.minimum(
            tf.reshape(sequence_length, [-1]),
            self._max_train_steps)
        tf.summary.scalar('max_sequence_length', tf.reduce_max(length_limit))
        split_limit = tf.maximum(length_limit - self._eval_steps, 2)
        start_limit = tf.maximum(split_limit - self._train_steps - 1, 0)
        train_loss = 0
        eval_loss = 0
        num_train_labels = 0
        num_eval_labels = 0
        updates = []
        for key in self._item_events:
            non_zeros_indices = tf.cast(tf.where(label_by_event[key] > 0), tf.int32)
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
            num_event_train_labels = tf.shape(train_indices)[0]
            num_event_eval_labels = tf.shape(eval_indices)[0]
            num_train_labels += num_event_train_labels
            num_eval_labels += num_event_eval_labels
            with tf.variable_scope(key):
                tf.summary.scalar('num_train_labels', num_event_train_labels)
                tf.summary.scalar('num_eval_labels', num_event_eval_labels)
                train_event_loss, _ = self._compute_event_loss(
                    rnn_output, train_indices, label_by_event[key], softmax_by_event[key])
                eval_event_loss, metric_update = self._compute_event_loss(
                    rnn_output, eval_indices, label_by_event[key],
                    softmax_by_event[key], metrics=self._eval_metrics)
            train_loss += train_event_loss
            eval_loss += eval_event_loss
            updates += metric_update
        train_loss = train_loss / tf.cast(tf.maximum(1, num_train_labels), tf.float64)
        eval_loss = eval_loss / tf.cast(tf.maximum(1, num_eval_labels), tf.float64)
        tf.summary.scalar('train_loss', train_loss)
        tf.summary.scalar('eval_loss', eval_loss)
        return train_loss, updates

    def _get_inputs(self):
        attr2input = {}
        max_seq_len = None
        for attr, config in self._attr2config.iteritems():
            with tf.variable_scope(attr):
                size = None
                if config['level'] == 'user':
                    size = 1
                inputs = tf.placeholder(
                    tf.int32, shape=(None, size), name='%s_idx' % attr)
                if config['level'] == 'item':
                    max_seq_len = tf.shape(attr)[1] / self._page_size
                if self._filter_unrecognized:
                    inputs = inputs * tf.cast(inputs < config['vocab_size'], tf.int32)
                attr2input[attr] = inputs
        if max_seq_len is None:
            raise Exception('There must be an item level attribute in attr2config.')
        sequence_length = tf.placeholder(tf.int32, shape=(None, 1), name='sequence_length_val')
        return max_seq_len, sequence_length, attr2input

    def _get_embedders(self):
        attr2embedder = {}
        for attr, config in self._attr2config.iteritems:
            attr2embedder[attr] = tf.keras.layers.Embedding(config['vocab_size'], config['embedding_dim'])
        return attr2embedder

    def _get_embeddings(self, max_seq_len, attr2input, attr2embedder):
        attr2embedding = {}
        for attr, config in self._attr2config.iteritems():
            if config['level'] == 'item':
                inputs = tf.reshape(attr2input[attr], [tf.shape(attr)[0], max_seq_len, self._page_size])
                embedding = attr2embedder[attr](inputs)
                embedding = tf.reshape(
                    tf.shape(embedding)[0], tf.shape(embedding)[1], self._page_size * config['embedding_dim'])
            else:
                inputs = attr2input[attr]
                embedding = attr2embedder[attr](inputs)
            attr2embedding[attr] = embedding
        return attr2embedding

    def _get_user_model(self, attr2embedding):
        pass

    def _get_loss(self, sequence_length, user_model, attr2input):
        pass

    def _get_prediction(self, sequence_length, user_model, output_paras):
        return self._get_softmax_prediction(sequence_length, user_model, output_paras)

    def _get_softmax_prediction(self, sequence_length, user_model, target2softmax):
        seq_idx = sequence_length - 1
        batch_idx = tf.expand_dims(tf.range(tf.shape(sequence_length)[0]), 1)
        output_idx = tf.concat([batch_idx, seq_idx], 1)
        sequence_output = tf.gather_nd(user_model, output_idx)
        target2preds = {}
        for target in self._target2config:
            logits = target2softmax[target](sequence_output)
            target2preds[target] = tf.nn.softmax(logits, name='%s_prob' % target)
        return target2preds

    def build_model(self):
        with tf.variable_scope('inputs'):
            max_seq_len, sequence_length, attr2input = self._get_inputs()
        with tf.variable_scope('embeddings'):
            attr2embedder = self._get_embedders()
            attr2embedding = self._get_embeddings(max_seq_len, attr2input, attr2embedder)
        with tf.variable_scope('user_model'):
            user_model = self._get_user_model(attr2embedding)
        with tf.variable_scope('loss'):
            loss, updates, output_paras = self._get_loss(
                sequence_length, user_model, attr2input)
        with tf.variable_scope('prediction'):
            self._get_prediction(sequence_length, user_model, output_paras)
        return loss, updates
