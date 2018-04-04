
import random
import tensorflow as tf

from src.models.prediction_model import PredictionModel


class BPRPredictionModel(PredictionModel):

    def __init__(self, context_attr, context_size, config=None):
        self._context_attr = context_attr
        self._context_size = context_size
        if config is None:
            self._sigmoid_config = {
                'item': {
                    'vocab_size':  10,
                    'sigmoid_dim': 10,
                    'attrs': {
                        'tag': [random.randint(0, 5) for _ in range(10)],
                        'genre': [random.randint(0, 3) for _ in range(10)],
                    }
                },
                'tag': {
                    'vocab_size':  3,
                    'sigmoid_dim': 10,
                },
                'genre': {
                    'vocab_size':  3,
                    'sigmoid_dim': 10,
                },
            }
        else:
            self._sigmoid_config = config

    def _get_sigmoid_paras(self, target, target_sigmoid):
        weights = tf.get_variable(
            '%s_weights' % target,
            shape=[target_sigmoid['vocab_size'], target_sigmoid['sigmoid_dim']],
            dtype=tf.float32, initializer=tf.truncated_normal_initializer)
        biases = tf.get_variable(
            '%s_biases' % target, shape=[target_sigmoid['vocab_size']],
            dtype=tf.float32, initializer=tf.zeros_initializer)
        if 'attrs' in target_sigmoid:
            for attr, item2attr in target_sigmoid['attrs'].iteritems():
                feamap = tf.constant(item2attr)
                attr_config = self._sigmoid_config[attr]
                attr_sigmoid = {
                    'weights': tf.get_variable(
                        '%s_weights' % attr,
                        shape=[attr_config['vocab_size'], attr_config['sigmoid_dim']],
                        dtype=tf.float32, initializer=tf.truncated_normal_initializer),
                    'biases': tf.get_variable(
                        '%s_biases' % attr, shape=[attr_config['vocab_size']],
                        dtype=tf.float32, initializer=tf.zeros_initializer),
                }
                weights += tf.gather(attr_sigmoid['weights'], feamap)
                biases += tf.gather(attr_sigmoid['biases'], feamap)
        return weights, biases

    def get_target_paras(self, target, config):
        target_sigmoid = self._sigmoid_config[target]
        weights, biases = self._get_sigmoid_paras(target, target_sigmoid)
        paras = {
            'weights': weights,
            'biases': biases,
        }
        return paras

    def get_target_loss(self, used_model, labels, indices, user_model,
                        paras, target, config, mode, context):
        mask = tf.gather_nd(labels, indices) > 0
        indices = tf.boolean_mask(indices, mask)
        used_model = tf.boolean_mask(used_model, mask)
        used_labels = tf.gather_nd(labels, indices)
        display = context[self._context_attr]
        batch_step = tf.slice(indices,
                              begin=[0, 0],
                              size=[tf.shape(indices)[0], 2])
        used_display = tf.reshape(
            tf.gather_nd(display, batch_step),
            [tf.shape(batch_step)[0], self._context_size])
        tiled_labels = tf.tile(
            tf.expand_dims(used_labels, axis=1),
            [1, self._context_size])
        tiled_model = tf.tile(
            tf.expand_dims(used_model, axis=1),
            [1, self._context_size, 1])

        weights = tf.gather(paras['weights'], tiled_labels) - tf.gather(paras['weights'], used_display)
        biases = tf.gather(paras['biases'], tiled_labels) - tf.gather(paras['biases'], used_display)
        logits = tf.reduce_sum(tiled_model * weights, axis=2) + biases

        loss_mask = tf.not_equal(tiled_labels, used_display)
        losses = tf.nn.sigmoid_cross_entropy_with_logits(labels=tf.ones_like(logits), logits=logits)
        losses = tf.boolean_mask(losses, loss_mask)
        loss = tf.reduce_sum(losses)
        return tf.size(losses), loss, []

    def get_target_prediction(self, used_model, paras, target, config):
        logits = tf.matmul(used_model, tf.transpose(paras['weights'])) + paras['biases']
        return tf.sigmoid(logits, name='%s_prob_op' % target)

    def get_item_prediction(self, used_model, paras, items, target, config):
        weights = tf.gather(paras['weights'], items)
        biases = tf.gather(paras['biases'], items)
        tiled_model = tf.tile(tf.expand_dims(used_model, 1), [1, tf.shape(items)[1], 1])
        logits = tf.reduce_sum(weights * tiled_model, axis=2) + biases
        return tf.sigmoid(logits, name='%s_items_prob_op' % target)
