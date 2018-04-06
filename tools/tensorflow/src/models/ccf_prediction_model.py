
import random
import tensorflow as tf

from src.models.prediction_model import PredictionModel


class CCFSoftmaxModel(PredictionModel):

    def __init__(self, user_attr, user_vocab_size, context_attr, context_size, config=None):
        self._user_attr = user_attr
        self._user_vocab_size = user_vocab_size
        self._context_attr = context_attr
        self._context_size = context_size
        if config is None:
            self._softmax_config = {
                'item': {
                    'vocab_size':  10,
                    'softmax_dim': 10,
                    'attrs': {
                        'tag': [random.randint(0, 5) for _ in range(10)],
                        'genre': [random.randint(0, 3) for _ in range(10)],
                    }
                },
                'tag': {
                    'vocab_size':  3,
                    'softmax_dim': 10,
                },
                'genre': {
                    'vocab_size':  3,
                    'softmax_dim': 10,
                },
            }
        else:
            self._softmax_config = config

    def _get_softmax_paras(self, target, target_softmax):
        weights = tf.get_variable(
            '%s_weights' % target,
            shape=[target_softmax['vocab_size'], target_softmax['softmax_dim']],
            dtype=tf.float32, initializer=tf.truncated_normal_initializer)
        biases = tf.get_variable(
            '%s_biases' % target, shape=[target_softmax['vocab_size']],
            dtype=tf.float32, initializer=tf.zeros_initializer)
        if 'attrs' in target_softmax:
            for attr, item2attr in target_softmax['attrs'].iteritems():
                feamap = tf.constant(item2attr)
                attr_config = self._softmax_config[attr]
                attr_softmax = {
                    'weights': tf.get_variable(
                        '%s_weights' % attr,
                        shape=[attr_config['vocab_size'], attr_config['softmax_dim']],
                        dtype=tf.float32, initializer=tf.truncated_normal_initializer),
                    'biases': tf.get_variable(
                        '%s_biases' % attr, shape=[attr_config['vocab_size']],
                        dtype=tf.float32, initializer=tf.zeros_initializer),
                }
                weights += tf.gather(attr_softmax['weights'], feamap)
                biases += tf.gather(attr_softmax['biases'], feamap)
        return weights, biases

    def get_target_paras(self, target, config):
        target_softmax = self._softmax_config[target]
        weights, biases = self._get_softmax_paras(target, target_softmax)
        inactions = tf.get_variable(
            'inactions', shape=[self._user_vocab_size],
            dtype=tf.float32, initializer=tf.zeros_initializer)
        tf.summary.histogram('inaction_biases', inactions)
        paras = {
            'weights': weights,
            'biases': biases,
            'inactions': inactions,
        }
        return paras

    def get_target_loss(self, used_model, labels, indices, user_model,
                        paras, target, config, mode, context):
        display = context[self._context_attr]
        used_display = tf.gather_nd(display, indices)
        weights = tf.gather(paras['weights'], used_display)
        biases = tf.gather(paras['biases'], used_display)
        logits = tf.reduce_sum(used_model * weights, axis=1) + biases
        logits = tf.reshape(logits, [tf.shape(logits)[0] / self._context_size, self._context_size])
        used_display = tf.reshape(used_display, [tf.shape(used_display)[0] / self._context_size, self._context_size])
        logits = logits * tf.cast(used_display > 0, tf.float32) - 1000.0 * tf.cast(used_display == 0, tf.float32)

        user = context[self._user_attr]
        batch_idx = tf.reshape(
            tf.slice(indices,
                     begin=[0, 0],
                     size=[tf.shape(indices)[0], 1]),
            [tf.shape(indices)[0] / self._context_size, self._context_size])
        batch_idx = tf.reshape(
            tf.slice(batch_idx,
                     begin=[0, 0],
                     size=[tf.shape(batch_idx)[0], 1]), [-1])
        used_user = tf.gather(tf.reshape(user, [-1]), batch_idx)
        inaction_logits = tf.gather(paras['inactions'], used_user)
        inaction_logits = tf.reshape(inaction_logits,
                                     [tf.shape(inaction_logits)[0], 1])

        extended_logits = tf.concat([logits, inaction_logits], axis=1)
        extended_probs = tf.nn.softmax(extended_logits)

        used_labels = tf.gather_nd(labels, indices)
        used_labels = tf.reshape(used_labels, [tf.shape(used_labels)[0] / self._context_size, self._context_size])
        used_mask = used_labels > 0
        inaction_mask = tf.reduce_all(tf.equal(used_labels, 0), axis=1)
        extended_mask = tf.concat([used_mask, tf.expand_dims(inaction_mask, 1)], axis=1)

        probs = tf.boolean_mask(extended_probs, extended_mask),
        losses = -tf.log(tf.maximum(probs, 1e-07))
        loss = tf.reduce_sum(losses)
        return tf.size(losses), loss, []

    def get_target_prediction(self, used_model, paras, target, config):
        logits = tf.matmul(used_model, tf.transpose(paras['weights'])) + paras['biases']
        return tf.nn.softmax(logits, name='%s_prob_op' % target)
