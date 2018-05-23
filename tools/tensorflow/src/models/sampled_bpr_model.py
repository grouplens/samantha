
import tensorflow as tf

from src.models.prediction_model import SigmoidPredictionModel


class SampledBPRModel(SigmoidPredictionModel):

    def __init__(self, config=None):
        super(SampledBPRModel, self).__init__(config=config)

    def get_target_loss(self, used_model, labels, indices, user_model,
            paras, target, config, mode, context):
        target_config = self._config[target]
        mask = tf.gather_nd(labels, indices) > 0
        indices = tf.boolean_mask(indices, mask)
        used_labels = tf.gather_nd(labels, indices)
        used_model = tf.boolean_mask(used_model, mask)
        vocab_size = target_config['vocab_size']
        if 'num_sampled' in target_config:
            num_sampled = target_config['num_sampled']
        else:
            num_sampled = vocab_size
        if num_sampled >= vocab_size - 1:
            num_ids = vocab_size
            uniq_ids = tf.range(vocab_size)
        else:
            sampled_ids = tf.random_uniform([num_sampled],
                                            dtype=tf.int32, maxval=vocab_size - 1)
            all_ids = tf.concat([sampled_ids, used_labels], axis=0)
            uniq_ids, idx = tf.unique(all_ids)
            num_ids = tf.shape(uniq_ids)[0]
        tiled_uniqs = tf.tile(
            tf.expand_dims(uniq_ids, axis=0),
            [tf.shape(used_labels)[0], 1])
        tiled_labels = tf.tile(
            tf.expand_dims(used_labels, axis=1),
            [1, num_ids])
        tiled_model = tf.tile(
            tf.expand_dims(used_model, axis=1),
            [1, num_ids, 1])
        weights = tf.gather(paras['weights'], tiled_labels) - tf.gather(paras['weights'], tiled_uniqs)
        biases = tf.gather(paras['biases'], tiled_labels) - tf.gather(paras['biases'], tiled_uniqs)
        logits = tf.reduce_sum(tiled_model * weights, axis=2) + biases
        loss_mask = tf.not_equal(tiled_labels, tiled_uniqs)
        losses = tf.nn.sigmoid_cross_entropy_with_logits(labels=tf.ones_like(logits), logits=logits)
        losses = tf.boolean_mask(losses, loss_mask)
        loss = tf.reduce_sum(losses)
        return tf.size(losses), loss, []
