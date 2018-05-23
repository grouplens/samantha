
import tensorflow as tf

from src.models.prediction_model import BasicPredictionModel


class RegressionPredictionModel(BasicPredictionModel):

    def __init__(self, config=None):
        super(RegressionPredictionModel, self).__init__(config=config)

    def get_target_loss(self, used_model, labels, indices, user_model,
                        paras, target, config, mode, context):
        context_attr = self._config[target]['context']
        contexts = context[context_attr]
        mask = tf.gather_nd(contexts, indices) > 0
        indices = tf.boolean_mask(indices, mask)
        used_model = tf.boolean_mask(used_model, mask)
        used_contexts = tf.gather_nd(contexts, indices)
        used_labels = tf.gather_nd(labels, indices)
        weights = tf.gather(paras['weights'], used_contexts)
        biases = tf.gather(paras['biases'], used_contexts)
        preds = tf.reduce_sum(used_model * weights, axis=1) + biases
        preds = self._get_display_preds(preds, indices, paras, target, context)
        loss = tf.nn.l2_loss(used_labels - preds)
        return tf.shape(used_labels)[0], loss, []

