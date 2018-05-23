
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
            logits = self._get_vocab_preds(logits, indices, paras, target, context)
            losses = tf.nn.sparse_softmax_cross_entropy_with_logits(
                labels=used_labels, logits=logits)
        else:
            num_consts = 0
            if 'user_bias' in paras:
                user_bias = self._get_user_bias(indices, paras, target, context)
                used_model = tf.concat([
                    used_model, tf.expand_dims(user_bias, axis=1)
                ], axis=1)
                num_consts += 1
            if 'global_bias' in paras:
                used_model = tf.concat([
                    used_model, tf.tile(tf.expand_dims(paras['global_bias'], axis=1), [tf.shape(used_model)[0], 1])
                ], axis=1)
                num_consts += 1
            weights = paras['weights']
            if num_consts > 0:
                consts = tf.ones([target_config['vocab_size'], num_consts])
                weights = tf.concat([weights, consts], axis=1)
            losses = tf.nn.sampled_softmax_loss(weights, paras['biases'],
                                                tf.expand_dims(used_labels, 1), used_model,
                                                target_config['num_sampled'], target_config['vocab_size'])
        loss = tf.reduce_sum(losses)
        return tf.shape(used_labels)[0], loss, []

    def get_target_prediction(self, used_model, indices, paras, target, config, context):
        logits = self._get_raw_prediction(used_model, indices, paras, target, context)
        return tf.nn.softmax(logits, name='%s_pred_op' % target)
