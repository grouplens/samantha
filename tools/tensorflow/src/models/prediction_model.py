
import tensorflow as tf

class PredictionModel:

    def __init__(self):
        pass

    def get_target_paras(self, target, config):
        raise Exception('This must be overridden.')

    def get_target_loss(self, used_model, labels, indices, user_model,
            paras, target, config, mode, context):
        raise Exception('This must be overridden.')

    def get_target_prediction(self, used_model, paras, target, config):
        raise Exception('This must be overridden.')

    def get_item_prediction(self, used_model, paras, items, target, config):
        preds = self.get_target_prediction(used_model, paras, target, config)
        batch_range = tf.range(tf.shape(preds)[0])
        tiled_batch = tf.tile(tf.expand_dims(batch_range, axis=1), [1, tf.shape(items)[1]])
        indices = tf.concat([
            tf.expand_dims(tiled_batch, axis=2),
            tf.expand_dims(items, axis=2)
        ], axis=2)
        return tf.gather_nd(preds, indices)
