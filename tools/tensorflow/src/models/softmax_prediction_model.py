
import tensorflow as tf

from src.models.prediction_model import PredictionModel


class SoftmaxPredictionModel(PredictionModel):

    def __init__(self, softmax_dim):
        self._softmax_dim = softmax_dim

    def _get_target_paras(self, target, config):
        softmax = tf.keras.layers.Dense(self._softmax_dim, dtype=tf.float32)
        return softmax

    def _get_target_prediction_loss(self, user_model, labels, softmax, target, config):
        logits = softmax(user_model)
        losses = tf.nn.sparse_softmax_cross_entropy_with_logits(
            labels=labels, logits=logits)
        loss = tf.reduce_sum(losses)
        return logits, loss

    def _get_target_prediction(self, model_output, softmax, target2preds, target, config):
        logits = softmax(model_output)
        target2preds[target] = tf.nn.softmax(logits, name='%s_prob' % target)
