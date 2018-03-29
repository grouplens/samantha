
import random
import tensorflow as tf

from src.models.prediction_model import PredictionModel


class CTRPredictionModel(PredictionModel):

    def __init__(self, context_attr, config=None):
        self._context_attr = context_attr
        if config is None:
            self._sigmoid_config = {
                'item': {
                    'vocab_size':  10,
                    'sigmoid_dim': 10,
                    'attrs': {
                        'tag': [random.randint(0, 5) for _ in range(10)],
                        'genre': [random.randint(0, 3) for _ in range(10)],
                    }
                },
                'tag': {
                    'vocab_size':  3,
                    'sigmoid_dim': 10,
                },
                'genre': {
                    'vocab_size':  3,
                    'sigmoid_dim': 10,
                },
            }
        else:
            self._sigmoid_config = config

    def _get_sigmoid_paras(self, target, target_sigmoid):
        weights = tf.get_variable(
            '%s_weights' % target,
            shape=[target_sigmoid['vocab_size'], target_sigmoid['sigmoid_dim']],
            dtype=tf.float32, initializer=tf.truncated_normal_initializer)
        biases = tf.get_variable(
            '%s_biases' % target, shape=[target_sigmoid['vocab_size']],
            dtype=tf.float32, initializer=tf.zeros_initializer)
        if 'attrs' in target_sigmoid:
            for attr, item2attr in target_sigmoid['attrs'].iteritems():
                feamap = tf.constant(item2attr)
                attr_config = self._sigmoid_config[attr]
                attr_sigmoid = {
                    'weights': tf.get_variable(
                        '%s_weights' % attr,
                        shape=[attr_config['vocab_size'], attr_config['sigmoid_dim']],
                        dtype=tf.float32, initializer=tf.truncated_normal_initializer),
                    'biases': tf.get_variable(
                        '%s_biases' % attr, shape=[attr_config['vocab_size']],
                        dtype=tf.float32, initializer=tf.zeros_initializer),
                }
                weights += tf.gather(attr_sigmoid['weights'], feamap)
                biases += tf.gather(attr_sigmoid['biases'], feamap)
        return weights, biases

    def get_target_paras(self, target, config):
        target_sigmoid = self._sigmoid_config[target]
        weights, biases = self._get_sigmoid_paras(target, target_sigmoid)
        paras = {
            'weights': weights,
            'biases': biases,
        }
        return paras

    def get_target_loss(self, used_model, labels, indices, user_model,
                        paras, target, config, mode, context):
        display = context[self._context_attr]
        mask = tf.gather_nd(display, indices) > 0
        indices = tf.boolean_mask(indices, mask)
        used_model = tf.boolean_mask(used_model, mask)
        used_display = tf.gather_nd(display, indices)
        weights = tf.gather(paras['weights'], used_display)
        biases = tf.gather(paras['biases'], used_display)
        logits = tf.reduce_sum(used_model * weights, axis=1) + biases
        used_labels = tf.gather_nd(labels, indices)
        used_labels = tf.cast(used_labels > 0, tf.float32)
        losses = tf.nn.sigmoid_cross_entropy_with_logits(labels=used_labels, logits=logits)
        loss = tf.reduce_sum(losses)
        return tf.size(losses), loss, []

    def get_target_prediction(self, used_model, paras, target, config):
        logits = tf.matmul(used_model, tf.transpose(paras['weights'])) + paras['biases']
        return tf.sigmoid(logits, name='%s_prob_op' % target)
