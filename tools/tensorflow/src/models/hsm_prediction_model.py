
import random
import tensorflow as tf

from src.models.prediction_model import PredictionModel


class HierarchicalPredictionModel(PredictionModel):

    def __init__(self, hierarchies=None, metrics='MAP@1'):
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
        self._metrics = metrics

    def _layer_wise_loss(self, cluster_vocab_size,
                         cluster_labels, item_labels, item2cluster,
                         weights, biases, user_model):
        uniq_clusters, _ = tf.unique(cluster_labels)
        whether_include_clusters = tf.sparse_to_dense(
            uniq_clusters, [cluster_vocab_size],
            tf.ones_like(uniq_clusters, dtype=tf.bool),
            default_value=False, validate_indices=False)
        whether_include_items = tf.gather(whether_include_clusters, item2cluster)
        included_items = tf.reshape(tf.where(whether_include_items), [-1])
        included_clusters = tf.gather(item2cluster, included_items)
        included_weights = tf.gather(weights, included_items)
        included_biases = tf.gather(biases, included_items)
        cluster_included_indices = tf.where(
            tf.equal(
                tf.tile(tf.expand_dims(cluster_labels, 1), [1, tf.shape(included_clusters)[0]]),
                tf.tile(tf.expand_dims(included_clusters, 0), [tf.shape(cluster_labels)[0], 1])
            )
        )
        cluster_idx = tf.reshape(
            tf.slice(
                cluster_included_indices,
                begin=[0, 0],
                size=[tf.shape(cluster_included_indices)[0], 1]
            ),
            [-1]
        )
        included_idx = tf.reshape(
            tf.slice(
                cluster_included_indices,
                begin=[0, 1],
                size=[tf.shape(cluster_included_indices)[0], 1]
            ),
            [-1]
        )
        included_model = tf.gather(user_model, cluster_idx)
        included_logits = tf.add(
            tf.reduce_sum(
                included_model * tf.gather(included_weights, included_idx), 1),
            tf.gather(included_biases, included_idx))
        exp_included_logits = tf.exp(included_logits)
        label_logits = tf.add(
            tf.reduce_sum(
                user_model * tf.gather(weights, item_labels), 1),
            tf.gather(biases, item_labels))
        included_sum_exp_logits = tf.segment_sum(exp_included_logits, cluster_idx)
        item_label_probs = tf.exp(label_logits) / included_sum_exp_logits
        item_label_losses = -tf.log(tf.maximum(item_label_probs, 1e-07))
        return item_label_probs, item_label_losses

    def _layer_wise_inference(self, cluster_probs, cluster_vocab_size,
                              user_model, item_weights, item_biases, item2cluster):
        logits = tf.matmul(user_model, tf.transpose(item_weights)) + item_biases
        exp_logits = tf.exp(logits)
        sum_exp_logits = tf.unsorted_segment_sum(exp_logits, item2cluster, cluster_vocab_size)
        item_sum_exp_logits = tf.gather(sum_exp_logits, item2cluster)
        item_cluster_probs = tf.transpose(tf.gather(cluster_probs, item2cluster, axis=1))
        within_cluster_probs = exp_logits / item_sum_exp_logits
        item_probs = item_cluster_probs * within_cluster_probs
        return item_probs

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

    def _compute_map_metrics(self, labels, logits, metric):
        K = metric.split('@')[1].split(',')
        updates = []
        for k in K:
            with tf.variable_scope('MAP_K%s' % k):
                map_value, map_update = tf.metrics.sparse_average_precision_at_k(
                    tf.cast(labels, tf.int64), logits, int(k))
                updates.append(map_update)
                tf.summary.scalar('MAP_K%s' % k, map_value)
        return updates

    def get_target_prediction_loss(self, user_model, labels, paras, target, config, mode):
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
        updates = []
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
                layer_preds, layer_losses = self._layer_wise_loss(
                    parent_level['vocab_size'], hierarchical_labels[parent_level['attr']],
                    hierarchical_labels[level['attr']], item2cluster[level['attr']],
                    weights[level['attr']], biases[level['attr']], user_model)
                preds *= layer_preds
                losses += layer_losses
            if mode == 'eval' and i < len(hierarchy) - 1 and self._metrics is not None:
                for metric in self._metrics.split(' '):
                    if 'MAP' in metric:
                        with tf.variable_scope(level['attr']):
                            updates += self._compute_map_metrics(
                                hierarchical_labels[level['attr']], preds, metric)
        loss = tf.reduce_sum(losses)
        return preds, loss, updates

    def get_target_prediction(self, user_model, paras, target, config):
        weights = paras['weights']
        biases = paras['biases']
        item2cluster = paras['item2cluster']
        hierarchy = self._hierarchies[target]
        preds = None
        for i in range(len(hierarchy)):
            level = hierarchy[i]
            if i == 0:
                logits = tf.matmul(user_model, tf.transpose(weights[level['attr']])) + biases[level['attr']]
                preds = tf.nn.softmax(logits)
            else:
                parent_level = hierarchy[i-1]
                preds = self._layer_wise_inference(
                    preds, parent_level['vocab_size'], user_model,
                    weights[level['attr']], biases[level['attr']],
                    item2cluster[level['attr']])
        return preds
