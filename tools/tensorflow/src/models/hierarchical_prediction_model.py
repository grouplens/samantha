
import random
import tensorflow as tf

from src.models.prediction_model import PredictionModel


class HierarchicalPredictionModel(PredictionModel):

    def __init__(self, hierarchies=None):
        if hierarchies is None:
            self._hierarchies = hierarchies
        else:
            self._hierarchies = {
                'item': [
                    {
                        'attr': 'cluster',
                        'vocab_size': 10,
                    }, {
                        'attr': 'item',
                        'vocab_size': 20,
                        'item2cluster': [random.randint(0, 9) for _ in range(20)]
                    }
                ]
            }

    def _layer_wise_loss(self):
        pass

    def _layer_wise_inference(self):
        pass

    def get_target_paras(self, target, config):
        softmax = {}
        item2cluster = {}
        for i in range(len(self._hierarchies)):
            level = hierarchy[i]
            softmax[level['attr']] = tf.keras.layers.Dense(level['vocab_size'], dtype=tf.float32)
            if i == 1:
                item2cluster[level['attr']] = tf.constant(level['item2cluster'])
        paras = {'softmax': softmax, 'item2cluster': item2cluster}
        return paras

    def get_target_prediction_loss(self, user_model, labels, softmax, target, config):
        logits = softmax(user_model)
        losses = tf.nn.sparse_softmax_cross_entropy_with_logits(
            labels=labels, logits=logits)
        loss = tf.reduce_sum(losses)
        return logits, loss

    def get_target_prediction(self, user_model, softmax, target2preds, target, config):
        logits = softmax(user_model)
        target2preds[target] = tf.nn.softmax(logits, name='%s_prob' % target)
