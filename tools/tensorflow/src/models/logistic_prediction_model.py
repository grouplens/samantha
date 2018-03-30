
import random
import tensorflow as tf

from src.models.prediction_model import PredictionModel


class LogisticPredictionModel(PredictionModel):

    def __init__(self, config=None):
        if config is None:
            self._logistic_config = {
                'item': {
                    'vocab_size':  10,
                    'logistic_dim': 10,
                    'num_sampled': 10,
                    'attrs': {
                        'tag': [random.randint(0, 5) for _ in range(10)],
                        'genre': [random.randint(0, 3) for _ in range(10)],
                    }
                },
                'tag': {
                    'vocab_size':  3,
                    'logistic_dim': 10,
                },
                'genre': {
                    'vocab_size':  3,
                    'logistic_dim': 10,
                }
            }
        else:
            self._logistic_config = config
            for key, val in self._logistic_config.iteritems():
                if 'num_sampled' not in val:
                    val['num_sampled'] = val['vocab_size']

    def _get_logistic_paras(self, target, target_logistic):
        weights = tf.get_variable(
            '%s_weights' % target,
            shape=[target_logistic['vocab_size'], target_logistic['logistic_dim']],
            dtype=tf.float32, initializer=tf.truncated_normal_initializer)
        biases = tf.get_variable(
            '%s_biases' % target, shape=[target_logistic['vocab_size']],
            dtype=tf.float32, initializer=tf.zeros_initializer)
        if 'attrs' in target_logistic:
            for attr, item2attr in target_logistic['attrs'].iteritems():
                feamap = tf.constant(item2attr)
                attr_config = self._logistic_config[attr]
                attr_logistic = {
                    'weights': tf.get_variable(
                        '%s_weights' % attr,
                        shape=[attr_config['vocab_size'], attr_config['logistic_dim']],
                        dtype=tf.float32, initializer=tf.truncated_normal_initializer),
                    'biases': tf.get_variable(
                        '%s_biases' % attr, shape=[attr_config['vocab_size']],
                        dtype=tf.float32, initializer=tf.zeros_initializer),
                }
                weights += tf.gather(attr_logistic['weights'], feamap)
                biases += tf.gather(attr_logistic['biases'], feamap)
        return weights, biases

    def get_target_paras(self, target, config):
        target_logistic = self._logistic_config[target]
        weights, biases = self._get_logistic_paras(target, target_logistic)
        paras = {
            'weights': weights,
            'biases': biases,
        }
        return paras

    def get_target_loss(self, used_model, labels, indices, user_model,
            paras, target, config, mode, context):
        target_logistic = self._logistic_config[target]
        mask = tf.gather_nd(labels, indices) > 0
        indices = tf.boolean_mask(indices, mask)
        used_labels = tf.gather_nd(labels, indices)
        used_model = tf.boolean_mask(used_model, mask)

        num_sampled = target_logistic['num_sampled']
        vocab_size = target_logistic['vocab_size']
        if num_sampled >= vocab_size - 1:
            logits = tf.matmul(used_model, tf.transpose(paras['weights'])) + paras['biases']
            label_index = tf.concat([
                tf.expand_dims(tf.range(tf.shape(logits)[0]), 1),
                tf.expand_dims(used_labels, 1)
            ], 1)
            num_ids = vocab_size
        else:
            sampled_ids = tf.random_uniform([num_sampled],
                                            dtype=tf.int32, maxval=vocab_size - 1)
            all_ids = tf.concat([sampled_ids, used_labels], axis=0)
            label_range = tf.range(num_sampled, num_sampled + tf.shape(used_labels)[0])
            uniq_ids, idx = tf.unique(all_ids)
            label_col_idx = tf.gather(idx, label_range)
            label_index = tf.concat([
                tf.expand_dims(tf.range(tf.shape(used_labels)[0]), 1),
                tf.expand_dims(label_col_idx, 1),
            ], axis=1)
            weights = tf.gather(paras['weights'], uniq_ids)
            biases = tf.gather(paras['biases'], uniq_ids)
            logits = tf.matmul(used_model, tf.transpose(weights)) + biases
            num_ids = tf.shape(uniq_ids)[0]

        binary_labels = tf.sparse_to_dense(
            label_index, [tf.shape(logits)[0], num_ids],
            tf.ones_like(used_labels, dtype=tf.float32),
            default_value=0.0, validate_indices=False)
        losses = tf.nn.sigmoid_cross_entropy_with_logits(labels=binary_labels, logits=logits)
        loss = tf.reduce_sum(losses)
        return tf.size(losses), loss, []

    def get_target_prediction(self, used_model, paras, target, config):
        logits = tf.matmul(used_model, tf.transpose(paras['weights'])) + paras['biases']
        return tf.sigmoid(logits, name='%s_prob_op' % target)
