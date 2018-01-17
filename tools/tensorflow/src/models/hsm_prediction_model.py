
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

    def _layer_wise_loss(self, cluster_vocab_size,
                         cluster_labels, item_labels, item2cluster,
                         weights, biases, user_model):
        uniq_clusters = tf.unique(cluster_labels)
        whether_include_clusters = tf.sparse_to_dense(
            uniq_clusters, [cluster_vocab_size],
            tf.ones_like(uniq_clusters, dtype=tf.bool), default_value=False)
        whether_include_items = tf.gather(whether_include_clusters, item2cluster)
        included_items = tf.where(whether_include_items)
        included_clusters = tf.gather(item2cluster, included_items)
        included_weights = weights(included_items)
        included_biases = biases(included_items)
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
                size=[tf.shape(cluster_included_indices[0]), 1]
            ),
            [-1]
        )
        included_idx = tf.reshape(
            tf.slice(
                cluster_included_indices,
                begin=[0, 1],
                size=[tf.shape(cluster_included_indices[0]), 1]
            ),
            [-1]
        )
        label_included_indices = tf.where(
            tf.equal(
                tf.tile(tf.expand_dims(item_labels, 1), [1, tf.shape(included_items)[0]]),
                tf.tile(tf.expand_dims(included_items, 0), [tf.shape(item_labels)[0], 1])
            )
        )
        label_included_idx = tf.reshape(
            tf.slice(
                label_included_indices,
                begin=[0, 1],
                size=[tf.shape(label_included_indices[0]), 1]
            ),
            [-1]
        )
        included_logits = tf.add(
            tf.matmul(
                user_model, tf.transpose(tf.gather(included_weights, included_idx))),
            tf.gather(included_biases, included_idx))
        exp_included_logits = tf.exp(included_logits)
        label_logits = tf.add(
            tf.matmul(
                user_model, tf.transpose(tf.gather(included_weights, label_included_idx))),
            tf.gather(included_biases, label_included_idx))
        included_sum_exp_logits = tf.segment_sum(exp_included_logits, cluster_idx)
        label_probs = tf.exp(label_logits) / included_sum_exp_logits
        label_losses = -tf.log(label_probs)
        return label_probs, label_losses

    def _layer_wise_inference(self):
        pass

    def get_target_paras(self, target, config):
        weights = {}
        biases = {}
        item2cluster = {}
        hierarchy = self._hierarchies[target]
        for i in range(len(hierarchy)):
            level = hierarchy[i]
            weights[level['attr']] = tf.keras.layers.Embedding(
                level['vocab_size'], dtype=tf.float32)
            biases[level['attr']] = tf.keras.layers.Embedding(
                1, dtype=tf.float32)
            if i == 1:
                item2cluster[level['attr']] = tf.constant(level['item2cluster'])
        paras = {'weights': weights, 'biases': biases, 'item2cluster': item2cluster}
        return paras

    def get_target_prediction_loss(self, user_model, labels, paras, target, config):
        preds = None
        loss = None
        return preds, loss

    def get_target_prediction(self, user_model, paras, target2preds, target, config):
        preds = None
        target2preds[target] = preds
