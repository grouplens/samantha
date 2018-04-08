
import random
import tensorflow as tf

from src.models import metrics, hsm
from src.models.prediction_model import PredictionModel


class HierarchicalPredictionModel(PredictionModel):

    def __init__(self, hierarchies=None, eval_metrics='MAP@1,5', eval_per_step=True):
        if hierarchies is not None:
            self._hierarchies = hierarchies
            for key, vals in self._hierarchies.iteritems():
                for val in vals:
                    if 'num_sampled' not in val:
                        val['num_sampled'] = val['vocab_size']
        else:
            self._hierarchies = {
                'item': [
                    {
                        'attr': 'cluster',
                        'vocab_size': 10,
                        'softmax_dim': 10,
                        'num_sampled': 4,
                    }, {
                        'attr': 'item',
                        'vocab_size': 20,
                        'softmax_dim': 10,
                        'item2cluster': [random.randint(0, 9) for _ in range(20)],
                        'num_sampled': 5,
                    }
                ]
            }
        self._eval_metrics = eval_metrics
        self._eval_per_step = eval_per_step

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

    def _compute_metrics(self, paras, attr2labels, labels,
            indices, user_model, target, config, context):
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
                if not self._eval_per_step:
                    used_model, uniq_batch_idx, ori_batch_idx, step_idx = metrics.get_per_batch_eval_user_model(
                            user_model, masked_indices)
                    attr2preds = self._compute_predictions(used_model, paras, target, limit=i)
                    updates += metrics.compute_per_batch_eval_metrics(
                            self._eval_metrics, attr2preds[attr], masked_labels, labels,
                            masked_indices, uniq_batch_idx, ori_batch_idx, step_idx,
                            config, context)
                else:
                    used_model = metrics.get_per_step_eval_user_model(user_model, masked_indices)
                    attr2preds = self._compute_predictions(used_model, paras, target, limit=i)
                    updates += metrics.compute_per_step_eval_metrics(
                        self._eval_metrics, attr2preds[attr], masked_labels,
                        masked_indices, config, context)
        return updates

    def get_target_loss(self, used_model, labels, indices, user_model,
            paras, target, config, mode, context):
        weights = paras['weights']
        biases = paras['biases']
        item2cluster = paras['item2cluster']
        hierarchy = self._hierarchies[target]

        mask = tf.gather_nd(labels, indices) > 0
        indices = tf.boolean_mask(indices, mask)
        used_labels = tf.gather_nd(labels, indices)
        used_model = tf.boolean_mask(used_model, mask)

        hierarchical_labels = {
            hierarchy[-1]['attr']: used_labels
        }
        for i in range(len(hierarchy) - 2, -1, -1):
            level = hierarchy[i]
            child_level = hierarchy[i+1]
            hierarchical_labels[level['attr']] = tf.gather(
                item2cluster[child_level['attr']], hierarchical_labels[child_level['attr']])
        losses = 0.0
        for i in range(len(hierarchy)):
            level = hierarchy[i]
            if i == 0:
                if level['num_sampled'] >= level['vocab_size'] - 1:
                    logits = tf.matmul(used_model, tf.transpose(weights[level['attr']])) + biases[level['attr']]
                    losses = tf.nn.sparse_softmax_cross_entropy_with_logits(
                        labels=hierarchical_labels[level['attr']], logits=logits)
                else:
                    losses = tf.nn.sampled_softmax_loss(
                        weights[level['attr']], biases[level['attr']],
                        tf.expand_dims(hierarchical_labels[level['attr']], 1), used_model,
                        level['num_sampled'], level['vocab_size'])
            else:
                parent_level = hierarchy[i-1]
                num_sampled = None
                if 'num_sampled' in level and level['num_sampled'] < level['vocab_size']:
                    num_sampled = level['num_sampled']
                _, layer_losses = hsm.layer_wise_loss(
                    parent_level['vocab_size'], hierarchical_labels[parent_level['attr']],
                    hierarchical_labels[level['attr']], item2cluster[level['attr']],
                    weights[level['attr']], biases[level['attr']], used_model, num_sampled=num_sampled)
                losses += layer_losses
        updates = []
        if mode == 'eval' and self._eval_metrics is not None:
            updates = self._compute_metrics(paras, hierarchical_labels,
                                            labels, indices, user_model, target,
                                            config, context)
        loss = tf.reduce_sum(losses)
        return tf.shape(used_labels)[0], loss, updates

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
                preds = tf.nn.softmax(logits, name='%s_prob_op' % level['attr'])
            else:
                parent_level = hierarchy[i-1]
                preds = hsm.layer_wise_inference(
                    preds, parent_level['vocab_size'], used_model,
                    weights[level['attr']], biases[level['attr']],
                    item2cluster[level['attr']], level['attr'])
            target2preds[level['attr']] = preds
        return target2preds

    def get_target_prediction(self, used_model, indices, user_model, paras, target, config, context):
        target2preds = self._compute_predictions(used_model, paras, target)
        return target2preds[target]
