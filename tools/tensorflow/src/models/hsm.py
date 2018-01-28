
import tensorflow as tf


def layer_wise_loss(cluster_vocab_size,
                    cluster_labels, item_labels, item2cluster,
                    weights, biases, used_model):
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
    included_model = tf.gather(used_model, cluster_idx)
    included_logits = tf.add(
        tf.reduce_sum(
            included_model * tf.gather(included_weights, included_idx), 1),
        tf.gather(included_biases, included_idx))
    exp_included_logits = tf.exp(included_logits)
    label_logits = tf.add(
        tf.reduce_sum(
            used_model * tf.gather(weights, item_labels), 1),
        tf.gather(biases, item_labels))
    included_sum_exp_logits = tf.segment_sum(exp_included_logits, cluster_idx)
    item_label_probs = tf.exp(label_logits) / included_sum_exp_logits
    item_label_losses = -tf.log(tf.maximum(item_label_probs, 1e-07))
    return item_label_probs, item_label_losses


def layer_wise_inference(cluster_probs, cluster_vocab_size,
                         used_model, item_weights, item_biases, item2cluster):
    logits = tf.matmul(used_model, tf.transpose(item_weights)) + item_biases
    exp_logits = tf.transpose(tf.exp(logits))
    sum_exp_logits = tf.unsorted_segment_sum(
        exp_logits, item2cluster, cluster_vocab_size)
    item_sum_exp_logits = tf.gather(sum_exp_logits, item2cluster)
    within_cluster_probs = exp_logits / item_sum_exp_logits
    item_cluster_probs = tf.gather(tf.transpose(cluster_probs), item2cluster)
    item_probs = tf.transpose(item_cluster_probs * within_cluster_probs)
    return item_probs
