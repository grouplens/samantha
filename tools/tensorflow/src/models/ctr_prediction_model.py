
import tensorflow as tf

from src.models.prediction_model import SigmoidPredictionModel


class CTRPredictionModel(SigmoidPredictionModel):

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
        logits = self._get_display_preds(logits, indices, paras, target, context)
        used_labels = tf.gather_nd(labels, indices)
        used_labels = tf.cast(used_labels > 0, tf.float32)
        losses = tf.nn.sigmoid_cross_entropy_with_logits(labels=used_labels, logits=logits)
        loss = tf.reduce_sum(losses)
        return tf.size(losses), loss, []
