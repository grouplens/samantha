
import tensorflow as tf

from src.models.prediction_model import SigmoidPredictionModel


class BPRPredictionModel(SigmoidPredictionModel):

    def __init__(self, display_attr, display_size, config=None):
        super(BPRPredictionModel, self).__init__(config=config)
        self._display_attr = display_attr
        self._display_size = display_size

    def get_target_loss(self, used_model, labels, indices, user_model,
                        paras, target, config, mode, context):
        mask = tf.gather_nd(labels, indices) > 0
        indices = tf.boolean_mask(indices, mask)
        used_model = tf.boolean_mask(used_model, mask)
        used_labels = tf.gather_nd(labels, indices)
        display = context[self._display_attr]
        batch_step = tf.slice(indices,
                              begin=[0, 0],
                              size=[tf.shape(indices)[0], 2])
        used_display = tf.reshape(
            tf.gather_nd(display, batch_step),
            [tf.shape(batch_step)[0], self._display_size])
        tiled_labels = tf.tile(
            tf.expand_dims(used_labels, axis=1),
            [1, self._display_size])
        tiled_model = tf.tile(
            tf.expand_dims(used_model, axis=1),
            [1, self._display_size, 1])

        weights = tf.gather(paras['weights'], tiled_labels) - tf.gather(paras['weights'], used_display)
        biases = tf.gather(paras['biases'], tiled_labels) - tf.gather(paras['biases'], used_display)
        logits = tf.reduce_sum(tiled_model * weights, axis=2) + biases

        loss_mask = tf.not_equal(tiled_labels, used_display)
        losses = tf.nn.sigmoid_cross_entropy_with_logits(labels=tf.ones_like(logits), logits=logits)
        losses = tf.boolean_mask(losses, loss_mask)
        loss = tf.reduce_sum(losses)
        return tf.size(losses), loss, []
