
import tensorflow as tf

from src.models.prediction_model import BasicPredictionModel


class CTRPredictionModel(BasicPredictionModel):

    def __init__(self, display_attr, config=None):
        super(CTRPredictionModel, self).__init__(config=config)
        self._display_attr = display_attr

    def get_target_loss(self, used_model, labels, indices, user_model,
                        paras, target, config, mode, context):
        display = context[self._display_attr]
        mask = tf.gather_nd(display, indices) > 0
        indices = tf.boolean_mask(indices, mask)
        used_model = tf.boolean_mask(used_model, mask)
        used_display = tf.gather_nd(display, indices)
        weights = tf.gather(paras['weights'], used_display)
        biases = tf.gather(paras['biases'], used_display)
        logits = tf.reduce_sum(used_model * weights, axis=1) + biases

        if 'user_bias' in paras:
            label_batch_idx = tf.reshape(tf.slice(indices, begin=[0, 0], size=[tf.shape(indices)[0], 1]), [-1])
            user = tf.reshape(context[self._config[target]['user_bias']], [-1])
            label_user = tf.gather(user, label_batch_idx)
            user_bias = tf.gather(paras['user_bias'], label_user)
            logits += user_bias
        if 'global_bias' in paras:
            logits += paras['global_bias']

        used_labels = tf.gather_nd(labels, indices)
        used_labels = tf.cast(used_labels > 0, tf.float32)
        losses = tf.nn.sigmoid_cross_entropy_with_logits(labels=used_labels, logits=logits)
        loss = tf.reduce_sum(losses)
        return tf.size(losses), loss, []

    def get_target_prediction(self, used_model, paras, target, config):
        logits = tf.matmul(used_model, tf.transpose(paras['weights'])) + paras['biases']
        return tf.sigmoid(logits, name='%s_prob_op' % target)
