
import random
import tensorflow as tf

from src.models import metrics, hsm
from src.models.prediction_model import PredictionModel


class HierarchicalPredictionModel(PredictionModel):

    def __init__(self, hierarchies=None, eval_metrics='MAP@1,5'):
        if hierarchies is not None:
            self._hierarchies = hierarchies
        else:
            self._hierarchies = {
                'item': [
                    {
                        'attr': 'cluster',
                        'vocab_size': 10,
                        'softmax_dim': 10,
                    }, {
                        'attr': 'item',
                        'vocab_size': 20,
                        'softmax_dim': 10,
                        'item2cluster': [random.randint(0, 9) for _ in range(20)],
                        'sample_rate': 0.8,
                    }
                ]
            }
        self._eval_metrics = eval_metrics

    def get_target_paras(self, target, config):
        weights = {}
        biases = {}
        item2cluster = {}
        hierarchy = self._hierarchies[target]
        for i in range(len(hierarchy)):
            level = hierarchy[i]
            weights[level['attr']] = tf.get_variable(
                '%s_weights' % level['attr'], shape=[level['vocab_size'], level['softmax_dim']],
                dtype=tf.float32, initializer=tf.truncated_normal_initializer)
            biases[level['attr']] = tf.get_variable(
                '%s_biases' % level['attr'], shape=[level['vocab_size']],
                dtype=tf.float32, initializer=tf.zeros_initializer)
            if i >= 1:
                item2cluster[level['attr']] = tf.constant(level['item2cluster'])
        paras = {'weights': weights, 'biases': biases, 'item2cluster': item2cluster}
        return paras

    def _compute_metrics(self, paras, attr2labels, label_shape,
            indices, user_model, target):
        hierarchy = self._hierarchies[target]
        updates = []
        for i in range(len(hierarchy) - 1):
            level = hierarchy[i]
            attr = level['attr']
            with tf.variable_scope(attr):
                used_labels = attr2labels[attr]
                mask = (used_labels > 0)
                masked_labels = tf.boolean_mask(used_labels, mask)
                masked_indices = tf.boolean_mask(indices, mask)
                used_model, uniq_batch_idx, ori_batch_idx, step_idx = metrics.get_eval_user_model(
                        user_model, masked_indices)
                attr2preds = self._compute_predictions(used_model, paras, target, limit=i)
                updates += metrics.compute_eval_label_metrics(
                        self._eval_metrics, attr2preds[attr], masked_labels, label_shape,
                        masked_indices, uniq_batch_idx, ori_batch_idx, step_idx)
        return updates

    def get_target_loss(self, used_model, labels, label_shape, indices, user_model,
            paras, target, config, mode):
        weights = paras['weights']
        biases = paras['biases']
        item2cluster = paras['item2cluster']
        hierarchy = self._hierarchies[target]
        hierarchical_labels = {
            hierarchy[-1]['attr']: labels
        }
        for i in range(len(hierarchy) - 2, -1, -1):
            level = hierarchy[i]
            child_level = hierarchy[i+1]
            hierarchical_labels[level['attr']] = tf.gather(
                item2cluster[child_level['attr']], hierarchical_labels[child_level['attr']])
        losses = 0.0
        preds = 1.0
        for i in range(len(hierarchy)):
            level = hierarchy[i]
            if i == 0:
                logits = tf.matmul(used_model, tf.transpose(weights[level['attr']])) + biases[level['attr']]
                exp_logits = tf.exp(logits)
                sum_exp_logits = tf.reduce_sum(exp_logits, axis=1)
                vocab_idx = tf.expand_dims(hierarchical_labels[level['attr']], axis=1)
                label_idx = tf.expand_dims(tf.range(tf.shape(exp_logits)[0]), axis=1)
                label_exp_logits = tf.gather_nd(exp_logits, tf.concat([label_idx, vocab_idx], axis=1))
                preds = label_exp_logits / sum_exp_logits
                losses = -tf.log(tf.maximum(preds, 1e-07))
            else:
                parent_level = hierarchy[i-1]
                sample_rate = 1.0
                if 'sample_rate' in level:
                    sample_rate = level['sample_rate']
                layer_preds, layer_losses = hsm.layer_wise_loss(
                    parent_level['vocab_size'], hierarchical_labels[parent_level['attr']],
                    hierarchical_labels[level['attr']], item2cluster[level['attr']],
                    weights[level['attr']], biases[level['attr']], used_model, sample_rate=sample_rate)
                preds *= layer_preds
                losses += layer_losses
        updates = []
        if mode == 'eval' and self._eval_metrics is not None:
            updates = self._compute_metrics(paras, hierarchical_labels,
                                            label_shape, indices, user_model, target)
        loss = tf.reduce_sum(losses)
        return loss, updates

    def _compute_predictions(self, used_model, paras, target, limit=None):
        weights = paras['weights']
        biases = paras['biases']
        item2cluster = paras['item2cluster']
        hierarchy = self._hierarchies[target]
        preds = None
        target2preds = {}
        if limit is None:
            limit = len(hierarchy)
        else:
            limit += 1
        for i in range(limit):
            level = hierarchy[i]
            if i == 0:
                logits = tf.matmul(used_model, tf.transpose(weights[level['attr']])) + biases[level['attr']]
                preds = tf.nn.softmax(logits)
            else:
                parent_level = hierarchy[i-1]
                preds = hsm.layer_wise_inference(
                    preds, parent_level['vocab_size'], used_model,
                    weights[level['attr']], biases[level['attr']],
                    item2cluster[level['attr']])
            target2preds[level['attr']] = preds
        return target2preds

    def get_target_prediction(self, used_model, paras, target, config):
        target2preds = self._compute_predictions(used_model, paras, target)
        return target2preds[target]
