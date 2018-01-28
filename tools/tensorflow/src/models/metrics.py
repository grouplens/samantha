
import tensorflow as tf


def compute_map_metrics(labels, logits, metric):
    K = metric.split('@')[1].split(',')
    updates = []
    for k in K:
        with tf.variable_scope('MAP_K%s' % k):
            map_value, map_update = tf.metrics.sparse_average_precision_at_k(
                tf.cast(labels, tf.int64), logits, int(k))
            updates.append(map_update)
            tf.summary.scalar('MAP_K%s' % k, map_value)
    return updates
