
import tensorflow as tf

from src.models.prediction_model import BasicPredictionModel


class SoftmaxPredictionModel(BasicPredictionModel):

    def __init__(self, config=None):
        super(SoftmaxPredictionModel, self).__init__(config=config)

    def get_target_loss(self, used_model, labels, indices, user_model,
            paras, target, config, mode, context):
        target_config = self._config[target]
        mask = tf.gather_nd(labels, indices) > 0
        indices = tf.boolean_mask(indices, mask)
        used_labels = tf.gather_nd(labels, indices)
        used_model = tf.boolean_mask(used_model, mask)
        if 'num_sampled' not in target_config or target_config['num_sampled'] >= target_config['vocab_size'] - 1:
            logits = tf.matmul(used_model, tf.transpose(paras['weights'])) + paras['biases']
            losses = tf.nn.sparse_softmax_cross_entropy_with_logits(
                labels=used_labels, logits=logits)
        else:
            losses = tf.nn.sampled_softmax_loss(paras['weights'], paras['biases'],
                                                tf.expand_dims(used_labels, 1), used_model,
                                                target_config['num_sampled'], target_config['vocab_size'])
        loss = tf.reduce_sum(losses)
        return tf.shape(used_labels)[0], loss, []

    def get_target_prediction(self, used_model, indices, paras, target, config, context):
        logits = self._get_raw_prediction(used_model, indices, paras, target, context)
        return tf.nn.softmax(logits, name='%s_pred_op' % target)
