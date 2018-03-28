
import random
import tensorflow as tf

from src.models.prediction_model import PredictionModel


class RegressionPredictionModel(PredictionModel):

    def __init__(self, context_attr, context_size, config=None):
        self._context_attr = context_attr
        self._context_size = context_size
        if config is None:
            self._regression_config = {
                'item': {
                    'vocab_size':  10,
                    'regression_dim': 10,
                    'attrs': {
                        'tag': [random.randint(0, 5) for _ in range(10)],
                        'genre': [random.randint(0, 3) for _ in range(10)],
                    }
                },
                'tag': {
                    'vocab_size':  3,
                    'regression_dim': 10,
                },
                'genre': {
                    'vocab_size':  3,
                    'regression_dim': 10,
                },
            }
        else:
            self._regression_config = config

    def _get_regression_paras(self, target, target_regression):
        weights = tf.get_variable(
            '%s_weights' % target,
            shape=[target_regression['vocab_size'], target_regression['regression_dim']],
            dtype=tf.float32, initializer=tf.truncated_normal_initializer)
        biases = tf.get_variable(
            '%s_biases' % target, shape=[target_regression['vocab_size']],
            dtype=tf.float32, initializer=tf.zeros_initializer)
        if 'attrs' in target_regression:
            for attr, item2attr in target_regression['attrs'].iteritems():
                feamap = tf.constant(item2attr)
                attr_config = self._regression_config[attr]
                attr_regression = {
                    'weights': tf.get_variable(
                        '%s_weights' % attr,
                        shape=[attr_config['vocab_size'], attr_config['regression_dim']],
                        dtype=tf.float32, initializer=tf.truncated_normal_initializer),
                    'biases': tf.get_variable(
                        '%s_biases' % attr, shape=[attr_config['vocab_size']],
                        dtype=tf.float32, initializer=tf.zeros_initializer),
                }
                weights += tf.gather(attr_regression['weights'], feamap)
                biases += tf.gather(attr_regression['biases'], feamap)
        return weights, biases

    def get_target_paras(self, target, config):
        target_regression = self._regression_config[target]
        weights, biases = self._get_regression_paras(target, target_regression)
        paras = {
            'weights': weights,
            'biases': biases,
        }
        return paras

    def get_target_loss(self, used_model, labels, indices, user_model,
                        paras, target, config, mode, context):
        ratings = context[self._context_attr]
        mask = tf.gather_nd(labels, indices) > 0
        indices = tf.boolean_mask(indices, mask)
        used_ratings = tf.gather_nd(ratings, indices)
        used_labels = tf.gather_nd(labels, indices)
        weights = tf.gather(paras['weights'], used_labels)
        biases = tf.gather(paras['biases'], used_labels)
        preds = tf.reduce_sum(used_model * weights, axis=1) + biases
        losses = tf.nn.l2_loss(used_ratings - preds)
        loss = tf.reduce_sum(losses)
        return tf.size(losses), loss, []

    def get_target_prediction(self, used_model, paras, target, config):
        preds = tf.matmul(used_model, tf.transpose(paras['weights']))
        return tf.add(preds, paras['biases'], name='%s_pred_op' % target)
