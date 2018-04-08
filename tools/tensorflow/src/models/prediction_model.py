
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

    def get_target_prediction(self, used_model, indices,
                              paras, target, config, context):
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

    def _get_user_bias(self, indices, paras, target, context):
        batch_idx = tf.reshape(tf.slice(indices, begin=[0, 0], size=[tf.shape(indices)[0], 1]), [-1])
        user = tf.reshape(context[self._config[target]['user_bias']], [-1])
        batch_user = tf.gather(user, batch_idx)
        user_bias = tf.gather(paras['user_bias'], batch_user)
        return user_bias

    def _get_display_preds(self, preds, indices, paras, target, context):
        if 'user_bias' in paras:
            preds += self._get_user_bias(indices, paras, target, context)
        if 'global_bias' in paras:
            preds += paras['global_bias']
        return preds

    def _get_vocab_preds(self, preds, indices, paras, target, context):
        if 'user_bias' in paras:
            user_bias = self._get_user_bias(indices, paras, target, context)
            preds += tf.tile(tf.expand_dims(user_bias, axis=1), [1, tf.shape(preds)[1]])
        if 'global_bias' in paras:
            preds += paras['global_bias']
        return preds

    def _get_raw_prediction(self, used_model, indices, paras, target, context):
        preds = tf.matmul(used_model, tf.transpose(paras['weights'])) + paras['biases']
        return self._get_vocab_preds(preds, indices, paras, target, context)

    def get_target_prediction(self, used_model, indices, paras, target, config, context):
        preds = self._get_raw_prediction(used_model, indices, paras, target, context)
        return tf.add(preds, 0.0, name='%s_pred_op' % target)


class SigmoidPredictionModel(BasicPredictionModel):

    def __init__(self, config=None):
        super(SigmoidPredictionModel, self).__init__(config=config)

    def get_target_prediction(self, used_model, indices, paras, target, config, context):
        logits = self._get_raw_prediction(used_model, indices, paras, target, context)
        return tf.sigmoid(logits, name='%s_pred_op' % target)
