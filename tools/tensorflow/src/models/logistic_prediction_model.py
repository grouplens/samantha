
import tensorflow as tf

from src.models.prediction_model import SigmoidPredictionModel


class LogisticPredictionModel(SigmoidPredictionModel):

    def __init__(self, config=None):
        super(LogisticPredictionModel, self).__init__(config=config)

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
            logits = tf.matmul(used_model, tf.transpose(paras['weights'])) + paras['biases']
            label_index = tf.concat([
                tf.expand_dims(tf.range(tf.shape(logits)[0]), 1),
                tf.expand_dims(used_labels, 1)
            ], 1)
            num_ids = vocab_size
        else:
            sampled_ids = tf.random_uniform([num_sampled],
                                            dtype=tf.int32, maxval=vocab_size - 1)
            all_ids = tf.concat([sampled_ids, used_labels], axis=0)
            label_range = tf.range(num_sampled, num_sampled + tf.shape(used_labels)[0])
            uniq_ids, idx = tf.unique(all_ids)
            label_col_idx = tf.gather(idx, label_range)
            label_index = tf.concat([
                tf.expand_dims(tf.range(tf.shape(used_labels)[0]), 1),
                tf.expand_dims(label_col_idx, 1),
            ], axis=1)
            weights = tf.gather(paras['weights'], uniq_ids)
            biases = tf.gather(paras['biases'], uniq_ids)
            logits = tf.matmul(used_model, tf.transpose(weights)) + biases
            num_ids = tf.shape(uniq_ids)[0]
        logits = self._get_vocab_preds(logits, indices, paras, target, context)
        binary_labels = tf.sparse_to_dense(
            label_index, [tf.shape(logits)[0], num_ids],
            tf.ones_like(used_labels, dtype=tf.float32),
            default_value=0.0, validate_indices=False)
        losses = tf.nn.sigmoid_cross_entropy_with_logits(labels=binary_labels, logits=logits)
        loss = tf.reduce_sum(losses)
        return tf.size(losses), loss, []
