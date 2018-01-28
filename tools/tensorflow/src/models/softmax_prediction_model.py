
import random
import tensorflow as tf

from src.models.prediction_model import PredictionModel


class SoftmaxPredictionModel(PredictionModel):

    def __init__(self, config=None):
        if config is None:
            self._softmax_config = {
                'item': {
                    'vocab_size':  10,
                    'softmax_dim': 10,
                    'num_sampled': 10,
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
                }
            }
        else:
            self._softmax_config = config
            for key, val in self._softmax_config.iteritems():
                if 'num_sampled' not in val:
                    val['num_sampled'] = val['vocab_size']

    def _get_softmax_paras(self, target, target_softmax):
        weights = tf.get_variable(
            '%s_weights' % target,  # TODO: Note that this probably won't reuse variables because of naming scope.
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
                        dtype=tf.float32, initializer=tf.zeros_initializer)
                }
                weights += tf.gather(attr_softmax['weights'], feamap)
                biases += tf.gather(attr_softmax['biases'], feamap)
        return weights, biases

    def get_target_paras(self, target, config):
        target_softmax = self._softmax_config[target]
        weights, biases = self._get_softmax_paras(target, target_softmax)
        paras = {
            'weights': weights,
            'biases': biases,
        }
        return paras

    def get_target_loss(self, used_model, labels, label_shape, indices, user_model,
            paras, target, config, mode):
        target_softmax = self._softmax_config[target]
        logits = tf.matmul(used_model, tf.transpose(paras['weights'])) + paras['biases']
        if target_softmax['num_sampled'] >= target_softmax['vocab_size'] - 1:
            losses = tf.nn.sparse_softmax_cross_entropy_with_logits(
                labels=labels, logits=logits)
        else:
            losses = tf.nn.sampled_softmax_loss(paras['weights'], paras['biases'],
                    tf.expand_dims(labels, 1), used_model,
                    target_softmax['num_sampled'], target_softmax['vocab_size'])
        loss = tf.reduce_sum(losses)
        return loss, []

    def get_target_prediction(self, used_model, paras, target, config):
        logits = tf.matmul(used_model, tf.transpose(paras['weights'])) + paras['biases']
        return tf.nn.softmax(logits, name='%s_prob' % target)
