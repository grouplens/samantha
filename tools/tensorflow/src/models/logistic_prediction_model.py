
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
        # TODO: sample negatives like AUC metric, note the number of labels is more than the positive labels
        pass

    def get_target_prediction(self, used_model, paras, target, config):
        pass
