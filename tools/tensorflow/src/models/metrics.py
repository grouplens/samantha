
import tensorflow as tf


def compute_map_metrics(labels, logits, metric):
    K = metric.split('@')[1].split(',')
    updates = []
    for k in K:
        with tf.variable_scope('MAP_K%s' % k):
            map_value, map_update = tf.metrics.sparse_average_precision_at_k(
                labels, logits, int(k))
            updates.append(map_update)
            tf.summary.scalar('MAP_K%s' % k, map_value)
    return updates


def get_eval_user_model(user_model, indices):
    batch_idx = tf.reshape(
        tf.slice(indices,
                 begin=[0, 0],
                 size=[tf.shape(indices)[0], 1]), [-1])
    step_idx = tf.reshape(
        tf.slice(indices,
                 begin=[0, 1],
                 size=[tf.shape(indices)[0], 1]), [-1])
    uniq_batch_idx, ori_batch_idx = tf.unique(batch_idx)
    min_step_idx = tf.segment_min(step_idx, batch_idx)
    min_step_idx = tf.gather(min_step_idx, uniq_batch_idx)
    used_indices = tf.concat([
        tf.expand_dims(uniq_batch_idx, 1),
        tf.expand_dims(min_step_idx, 1),
    ], 1)
    used_model = tf.gather_nd(user_model, used_indices)
    return used_model, uniq_batch_idx, ori_batch_idx, step_idx


def compute_eval_label_metrics(metrics, predictions, used_labels, label_shape, indices,
        uniq_batch_idx, ori_batch_idx, step_idx):
    new_batch_idx = tf.gather(tf.range(tf.shape(uniq_batch_idx)[0]), ori_batch_idx)
    new_indices = tf.concat([
        tf.expand_dims(new_batch_idx, 1),
        tf.expand_dims(step_idx, 1),
        tf.slice(indices,
                 begin=[0, 2],
                 size=[tf.shape(indices)[0], 1])],
        axis=1)
    eval_labels = tf.sparse_reshape(
        tf.SparseTensor(
            tf.cast(new_indices, tf.int64),
            tf.cast(used_labels, tf.int64),
            tf.cast(
                [tf.shape(uniq_batch_idx)[0], label_shape[1], label_shape[2]],
                tf.int64)
            ),
        [tf.shape(uniq_batch_idx)[0], label_shape[1] * label_shape[2]])
    updates = []
    for metric in metrics.split(' '):
        if 'MAP' in metric:
            updates += compute_map_metrics(eval_labels, predictions, metric)
    return updates


def get_per_step_eval_user_model(user_model, indices):
    batch_idx = tf.reshape(
        tf.slice(indices,
                 begin=[0, 0],
                 size=[tf.shape(indices)[0], 1]),
        [tf.shape(indices)[0], 1])
    step_idx = tf.slice(indices,
                        begin=[0, 1],
                        size=[tf.shape(indices)[0], 1])
    used_step_idx = tf.reshape(step_idx, [tf.shape(indices)[0], 1])
    used_indices = tf.concat([batch_idx, used_step_idx], 1)
    return tf.gather_nd(user_model, used_indices)


def compute_per_step_eval_label_metrics(metrics, predictions, eval_labels):
    updates = []
    for metric in metrics.split(' '):
        if 'MAP' in metric:
            updates += compute_map_metrics(eval_labels, predictions, metric)
    return updates

