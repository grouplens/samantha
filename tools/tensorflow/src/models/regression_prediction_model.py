
import random
import tensorflow as tf

from src.models.prediction_model import PredictionModel


class RegressionPredictionModel(PredictionModel):

    def __init__(self, config=None):
        if config is None:
            self._regression_config = {
                'rating': {
                    'context': 'item',
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
        target_config = self._regression_config[target]
        weights, biases = self._get_regression_paras(target, target_config)
        paras = {
            'weights': weights,
            'biases': biases,
        }
        return paras

    def get_target_loss(self, used_model, labels, indices, user_model,
                        paras, target, config, mode, context):
        context_attr = self._regression_config[target]['context']
        contexts = context[context_attr]
        mask = tf.gather_nd(contexts, indices) > 0
        indices = tf.boolean_mask(indices, mask)

        used_model = tf.boolean_mask(used_model, mask)
        used_contexts = tf.gather_nd(contexts, indices)
        used_labels = tf.gather_nd(labels, indices)
        weights = tf.gather(paras['weights'], used_contexts)
        biases = tf.gather(paras['biases'], used_contexts)
        preds = tf.reduce_sum(used_model * weights, axis=1) + biases
        loss = tf.nn.l2_loss(used_labels - preds)
        return tf.shape(used_labels)[0], loss, []

    def get_target_prediction(self, used_model, paras, target, config):
        preds = tf.matmul(used_model, tf.transpose(paras['weights']))
        return tf.add(preds, paras['biases'], name='%s_pred_op' % target)
