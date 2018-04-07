
import random

import tensorflow as tf


class PredictionModel(object):

    def __init__(self):
        pass

    def get_target_paras(self, target, config):
        raise Exception('This must be overridden.')

    def get_target_loss(self, used_model, labels, indices, user_model,
            paras, target, config, mode, context):
        raise Exception('This must be overridden.')

    def get_target_prediction(self, used_model, paras, target, config):
        raise Exception('This must be overridden.')


class BasicPredictionModel(PredictionModel):

    def __init__(self, config=None):
        if config is None:
            self._config = {
                'item': {
                    'vocab_size':  10,
                    'embedding_dim': 10,
                    'item_attrs': {
                        'tag': [random.randint(0, 5) for _ in range(10)],
                        'genre': [random.randint(0, 3) for _ in range(10)],
                    },
                    'user_bias': 'user',
                    'global_bias': True
                },
                'tag': {
                    'vocab_size':  3,
                    'embedding_dim': 10,
                },
                'genre': {
                    'vocab_size':  3,
                    'embedding_dim': 10,
                },
                'user': {
                    'vocab_size': 5,
                }
            }
        else:
            self._config = config

    def get_target_paras(self, target, config):
        target_config = self._config[target]
        weights = tf.get_variable(
            '%s_weights' % target,
            shape=[target_config['vocab_size'], target_config['embedding_dim']],
            dtype=tf.float32, initializer=tf.truncated_normal_initializer)
        biases = tf.get_variable(
            '%s_biases' % target, shape=[target_config['vocab_size']],
            dtype=tf.float32, initializer=tf.zeros_initializer)
        if 'item_attrs' in target_config:
            for attr, item2attr in target_config['item_attrs'].iteritems():
                feamap = tf.constant(item2attr)
                attr_config = self._config[attr]
                attr_config = {
                    'weights': tf.get_variable(
                        '%s_weights' % attr,
                        shape=[attr_config['vocab_size'], attr_config['embedding_dim']],
                        dtype=tf.float32, initializer=tf.truncated_normal_initializer),
                    'biases': tf.get_variable(
                        '%s_biases' % attr, shape=[attr_config['vocab_size']],
                        dtype=tf.float32, initializer=tf.zeros_initializer),
                }
                weights += tf.gather(attr_config['weights'], feamap)
                biases += tf.gather(attr_config['biases'], feamap)
        paras = {
            'weights': weights,
            'biases': biases,
        }
        if 'user_bias' in target_config:
            paras['user_bias'] = tf.get_variable(
                '%s_user_bias' % target, shape=[self._config[target_config['user_bias']]['vocab_size']],
                dtype=tf.float32, initializer=tf.zeros_initializer)
        if 'global_bias' not in target_config or target_config['global_bias']:
            paras['global_bias'] = tf.get_variable(
                '%s_global_bias' % target, shape=[1],
                dtype=tf.float32, initializer=tf.zeros_initializer)
        return paras

    def get_item_prediction(self, used_model, paras, items, target, config):
        preds = self.get_target_prediction(used_model, paras, target, config)
        batch_range = tf.range(tf.shape(preds)[0])
        tiled_batch = tf.tile(tf.expand_dims(batch_range, axis=1), [1, tf.shape(items)[1]])
        indices = tf.concat([
            tf.expand_dims(tiled_batch, axis=2),
            tf.expand_dims(items, axis=2)
        ], axis=2)
        return tf.gather_nd(preds, indices)
