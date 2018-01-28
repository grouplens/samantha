
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
                        'item2cluster': [random.randint(0, 9) for _ in range(20)]
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

    def _compute_metrics(self, user_model, paras, labels, label_shape, indices, target):
        batch_idx = tf.reshape(
            tf.slice(indices,
                     begin=[0, 0],
                     size=[tf.shape(indices)[0], 1]), [-1])
        step_idx = tf.reshape(
            tf.slice(indices,
                     begin=[0, 1],
                     size=[tf.shape(indices)[0], 1]), [-1])
        uniq_batch_idx, _ = tf.unique(batch_idx)
        min_step_idx = tf.segment_min(step_idx, batch_idx)
        used_indices = tf.concat([
            tf.expand_dims(uniq_batch_idx, 1),
            tf.expand_dims(min_step_idx, 1),
        ], 1)
        used_model = tf.gather_nd(user_model, used_indices)
        predictions = self._compute_predictions(
            used_model, paras, target, last_layer=False)
        updates = []
        for attr, preds in predictions.iteritems():
            with tf.variable_scope(attr):
                used_labels = tf.gather_nd(labels[attr], indices)
                mask = (used_labels > 0)
                masked_labels = tf.boolean_mask(used_labels, mask)
                masked_indices = tf.boolean_mask(indices, mask)
                eval_labels = tf.sparse_reshape(
                    tf.SparseTensor(
                        tf.cast(masked_indices, tf.int64),
                        tf.cast(masked_labels, tf.int64),
                        tf.cast(label_shape, tf.int64)),
                    [label_shape[0], label_shape[1] * label_shape[2]])
                for metric in self._eval_metrics.split(' '):
                    if 'MAP' in metric:
                        updates += metrics.compute_map_metrics(eval_labels, preds, metric)
        return updates

    def get_target_loss(self, user_model, labels, label_shape, indices, paras, target, config, mode):
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
                logits = tf.matmul(user_model, tf.transpose(weights[level['attr']])) + biases[level['attr']]
                exp_logits = tf.exp(logits)
                sum_exp_logits = tf.reduce_sum(exp_logits, axis=1)
                vocab_idx = tf.expand_dims(hierarchical_labels[level['attr']], axis=1)
                label_idx = tf.expand_dims(tf.range(tf.shape(exp_logits)[0]), axis=1)
                label_exp_logits = tf.gather_nd(exp_logits, tf.concat([label_idx, vocab_idx], axis=1))
                preds = label_exp_logits / sum_exp_logits
                losses = -tf.log(tf.maximum(preds, 1e-07))
            else:
                parent_level = hierarchy[i-1]
                layer_preds, layer_losses = hsm.layer_wise_loss(
                    parent_level['vocab_size'], hierarchical_labels[parent_level['attr']],
                    hierarchical_labels[level['attr']], item2cluster[level['attr']],
                    weights[level['attr']], biases[level['attr']], user_model)
                preds *= layer_preds
                losses += layer_losses
        updates = []
        if mode == 'eval' and self._eval_metrics is not None:
            updates = self._compute_metrics(user_model, paras, hierarchical_labels,
                                            label_shape, indices, target)
        loss = tf.reduce_sum(losses)
        return loss, updates

    def _compute_predictions(self, user_model, paras, target, last_layer=True):
        weights = paras['weights']
        biases = paras['biases']
        item2cluster = paras['item2cluster']
        hierarchy = self._hierarchies[target]
        preds = None
        target2preds = {}
        limit = len(hierarchy)
        if not last_layer:
            limit -= 1
        for i in range(limit):
            level = hierarchy[i]
            if i == 0:
                logits = tf.matmul(user_model, tf.transpose(weights[level['attr']])) + biases[level['attr']]
                preds = tf.nn.softmax(logits)
            else:
                parent_level = hierarchy[i-1]
                preds = hsm.layer_wise_inference(
                    preds, parent_level['vocab_size'], user_model,
                    weights[level['attr']], biases[level['attr']],
                    item2cluster[level['attr']])
            target2preds[level['attr']] = preds
        return target2preds

    def get_target_prediction(self, user_model, paras, target, config):
        target2preds = self._compute_predictions(user_model, paras, target)
        return target2preds[target]
