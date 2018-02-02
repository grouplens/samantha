
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


def compute_auc_metric(uniq_batch_idx, batch_idx, used_labels, preds, num_used=4):
    mask = tf.reshape(batch_idx < num_used, [tf.shape(batch_idx)[0]])
    masked_idx = tf.boolean_mask(batch_idx, mask)
    masked_label = tf.boolean_mask(used_labels, mask)
    pred_mask = tf.reshape(tf.range(tf.shape(preds)[0]) < num_used, [tf.shape(preds)[0]])
    used_preds = tf.boolean_mask(preds, pred_mask)
    new_indices = tf.concat([
        tf.expand_dims(masked_idx, 1),
        tf.expand_dims(masked_label, 1)
    ], axis=1)
    num_positives = tf.shape(new_indices)[0]
    tf.summary.scalar('num_positives', num_positives)
    labels = tf.sparse_to_dense(
        new_indices, [
            tf.minimum(num_used, tf.shape(uniq_batch_idx)[0]),
            tf.shape(preds)[1]],
        tf.ones([num_positives], dtype=tf.bool),
        default_value=False, validate_indices=False)
    auc_value, auc_update = tf.metrics.auc(
        tf.reshape(labels, [-1]), tf.reshape(used_preds, [-1]))
    tf.summary.scalar('AUC', auc_value)
    return auc_update


def compute_batch_auc_metric(batch_idx, sp_labels, preds):
    new_indices = tf.concat([
        tf.expand_dims(tf.cast(batch_idx, tf.int64), 1),
        tf.expand_dims(sp_labels.values, 1)
    ], axis=1)
    labels = tf.sparse_to_dense(
        new_indices, [sp_labels.dense_shape[0], tf.shape(preds, out_type=tf.int64)[1]],
        tf.ones([tf.shape(new_indices)[0]], dtype=tf.bool),
        default_value=False, validate_indices=False)
    auc_value, auc_update = tf.metrics.auc(
        tf.reshape(labels, [-1]),
        tf.reshape(preds, [-1]))
    tf.summary.scalar('AUC', auc_value)
    return auc_update


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
        elif 'AUC' in metric:
            # updates.append(compute_batch_auc_metric(new_batch_idx, eval_labels, predictions))
            updates.append(compute_auc_metric(uniq_batch_idx, new_batch_idx, used_labels, predictions))
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
            updates += compute_map_metrics(
                tf.cast(eval_labels, tf.int64), predictions, metric)
    return updates

