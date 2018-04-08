
import tensorflow as tf

from src.models.prediction_model import BasicPredictionModel


class CCFSoftmaxModel(BasicPredictionModel):

    def __init__(self, user_attr, user_vocab_size, display_attr, display_size, config=None):
        super(CCFSoftmaxModel, self).__init__(config=config)
        self._user_attr = user_attr
        self._user_vocab_size = user_vocab_size
        self._display_attr = display_attr
        self._display_size = display_size

    def get_target_paras(self, target, config):
        paras = super(CCFSoftmaxModel, self).get_target_paras(target, config)
        inactions = tf.get_variable(
            'inactions', shape=[self._user_vocab_size],
            dtype=tf.float32, initializer=tf.zeros_initializer)
        tf.summary.histogram('inaction_biases', inactions)
        paras['inactions'] = inactions
        return paras

    def get_target_loss(self, used_model, labels, indices, user_model,
                        paras, target, config, mode, context):
        display = context[self._display_attr]
        used_display = tf.gather_nd(display, indices)
        weights = tf.gather(paras['weights'], used_display)
        biases = tf.gather(paras['biases'], used_display)
        logits = tf.reduce_sum(used_model * weights, axis=1) + biases
        logits = self._get_display_preds(logits, indices, paras, target, context)
        logits = tf.reshape(logits, [tf.shape(logits)[0] / self._display_size, self._display_size])
        used_display = tf.reshape(used_display, [tf.shape(used_display)[0] / self._display_size, self._display_size])
        logits = logits * tf.cast(used_display > 0, tf.float32) - 1000.0 * tf.cast(used_display == 0, tf.float32)

        user = context[self._user_attr]
        batch_idx = tf.reshape(
            tf.slice(indices,
                     begin=[0, 0],
                     size=[tf.shape(indices)[0], 1]),
            [tf.shape(indices)[0] / self._display_size, self._display_size])
        batch_idx = tf.reshape(
            tf.slice(batch_idx,
                     begin=[0, 0],
                     size=[tf.shape(batch_idx)[0], 1]), [-1])
        used_user = tf.gather(tf.reshape(user, [-1]), batch_idx)
        inaction_logits = tf.gather(paras['inactions'], used_user)
        inaction_logits = tf.reshape(inaction_logits,
                                     [tf.shape(inaction_logits)[0], 1])

        extended_logits = tf.concat([logits, inaction_logits], axis=1)
        extended_probs = tf.nn.softmax(extended_logits)

        used_labels = tf.gather_nd(labels, indices)
        used_labels = tf.reshape(used_labels, [tf.shape(used_labels)[0] / self._display_size, self._display_size])
        used_mask = used_labels > 0
        inaction_mask = tf.reduce_all(tf.equal(used_labels, 0), axis=1)
        extended_mask = tf.concat([used_mask, tf.expand_dims(inaction_mask, 1)], axis=1)

        probs = tf.boolean_mask(extended_probs, extended_mask),
        losses = -tf.log(tf.maximum(probs, 1e-07))
        loss = tf.reduce_sum(losses)
        return tf.size(losses), loss, []
