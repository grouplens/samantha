
import tensorflow as tf

from src.models.prediction_model import PredictionModel


class SoftmaxPredictionModel(PredictionModel):

    def __init__(self, config=None):
        if config is None:
            self._softmax_config = {
                'item': {
                    'vocab_size':  10,
                    'num_sampled': 10,
                    'softmax_dim': 10,
                }
            }
        else:
            self._softmax_config = config
            for key, val in self._softmax_config.iteritems():
                if 'num_sampled' not in val:
                    val['num_sampled'] = val['vocab_size']

    def get_target_paras(self, target, config):
        target_softmax = self._softmax_config[target]
        paras = {}
        paras['weights'] = tf.get_variable(
                '%s_weights' % target,
                shape=[target_softmax['vocab_size'], target_softmax['softmax_dim']],
                dtype=tf.float32, initializer=tf.truncated_normal_initializer)
        paras['biases'] = tf.get_variable(
                '%s_biases' % target, shape=[target_softmax['vocab_size']],
                dtype=tf.float32, initializer=tf.zeros_initializer)
        return paras

    def get_target_prediction_loss(self, user_model, labels, paras, target, config, mode):
        target_softmax = self._softmax_config[target]
        logits = tf.matmul(user_model, tf.transpose(paras['weights'])) + paras['biases']
        if target_softmax['num_sampled'] >= target_softmax['vocab_size'] - 1:
            losses = tf.nn.sparse_softmax_cross_entropy_with_logits(
                labels=labels, logits=logits)
        else:
            losses = tf.nn.sampled_softmax_loss(paras['weights'], paras['biases'],
                    tf.expand_dims(labels, 1), user_model,
                    target_softmax['num_sampled'], target_softmax['vocab_size'])
        loss = tf.reduce_sum(losses)
        return logits, loss, []

    def get_target_prediction(self, user_model, paras, target, config):
        logits = tf.matmul(user_model, tf.transpose(paras['weights'])) + paras['biases']
        return tf.nn.softmax(logits, name='%s_prob' % target)
