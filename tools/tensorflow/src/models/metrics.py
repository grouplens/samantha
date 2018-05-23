
import tensorflow as tf


def compute_map_metrics(labels, logits, metric):
    K = metric.split('@')[1].split(',')
    updates = []
    values = []
    for k in K:
        with tf.variable_scope('MAP_K%s' % k):
            map_value, map_update = tf.metrics.sparse_average_precision_at_k(
                labels, logits, int(k))
            updates.append(map_update)
            values.append(map_value)
            tf.summary.scalar('MAP_K%s' % k, map_value)
    return values, updates


def compute_ap_metrics(labels, logits, metric):
    K = metric.split('@')[1].split(',')
    updates = []
    values = []
    for k in K:
        with tf.variable_scope('AP_K%s' % k):
            ap_value, ap_update = tf.metrics.sparse_precision_at_k(
                labels, logits, int(k))
            updates.append(ap_update)
            values.append(ap_value)
            tf.summary.scalar('AP_K%s' % k, ap_value)
    return values, updates


def compute_ar_metrics(labels, logits, metric):
    K = metric.split('@')[1].split(',')
    updates = []
    values = []
    for k in K:
        with tf.variable_scope('AR_K%s' % k):
            ar_value, ar_update = tf.metrics.recall_at_k(
                labels, logits, int(k))
            updates.append(ar_update)
            values.append(ar_value)
            tf.summary.scalar('AR_K%s' % k, ar_value)
    return values, updates


def _get_sampled_for_auc(batch_idx, used_labels, used_preds, num_sampled):
    sampled_ids = tf.random_uniform([num_sampled], dtype=tf.int32, maxval=tf.shape(used_preds)[1] - 1)
    all_ids = tf.concat([sampled_ids, used_labels], axis=0)
    label_range = tf.range(num_sampled, num_sampled + tf.shape(used_labels)[0])
    uniq_ids, idx = tf.unique(all_ids)
    label_idx = tf.gather(idx, label_range)
    new_indices = tf.concat([
        tf.expand_dims(batch_idx, 1),
        tf.expand_dims(label_idx, 1),
    ], axis=1)
    return new_indices, tf.gather(used_preds, uniq_ids, axis=1)


def compute_auc_metric(used_indices, batch_idx, used_labels, preds, num_used=None, num_sampled=2000):
    if num_used is not None:
        mask = tf.reshape(batch_idx < num_used, [tf.shape(batch_idx)[0]])
        batch_idx = tf.boolean_mask(batch_idx, mask)
        used_labels = tf.boolean_mask(used_labels, mask)
        pred_mask = tf.reshape(tf.range(tf.shape(preds)[0]) < num_used, [tf.shape(preds)[0]])
        used_preds = tf.boolean_mask(preds, pred_mask)
    else:
        used_preds = preds
        num_used = tf.shape(used_indices)[0]
    if num_sampled is None:
        num_sampled = tf.shape(used_labels)[0]
    else:
        num_sampled *= tf.cast(tf.shape(used_labels)[0] > 0, tf.int32)
    new_indices, used_preds = _get_sampled_for_auc(batch_idx, used_labels, used_preds, num_sampled)
    num_positives = tf.shape(new_indices)[0]
    tf.summary.scalar('num_positives', num_positives)
    labels = tf.sparse_to_dense(
        new_indices, [
            tf.minimum(num_used, tf.shape(used_indices)[0]),
            tf.shape(used_preds)[1]],
        tf.ones([num_positives], dtype=tf.bool),
        default_value=False, validate_indices=False)
    auc_value, auc_update = tf.metrics.auc(
        tf.reshape(labels, [-1]), tf.reshape(used_preds, [-1]),
        num_thresholds=1000)
    tf.summary.scalar('AUC', auc_value)
    return auc_value, auc_update


def get_per_batch_eval_user_model(user_model, indices):
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
    return used_model, used_indices, ori_batch_idx, step_idx


def compute_mae_metric(used_labels, used_contexts, predictions):
    label_index = tf.concat([
        tf.expand_dims(tf.range(tf.shape(used_labels)[0]), 1),
        tf.expand_dims(used_contexts, 1)
    ], 1)
    preds = tf.gather_nd(predictions, label_index)
    mae_value, mae_update = tf.metrics.mean_absolute_error(used_labels, preds)
    tf.summary.scalar('MAE', mae_value)
    return mae_value, mae_update


def compute_rmse_metric(used_labels, used_contexts, predictions):
    label_index = tf.concat([
        tf.expand_dims(tf.range(tf.shape(used_labels)[0]), 1),
        tf.expand_dims(used_contexts, 1)
    ], 1)
    preds = tf.gather_nd(predictions, label_index)
    rmse_value, rmse_update = tf.metrics.root_mean_squared_error(used_labels, preds)
    tf.summary.scalar('RMSE', rmse_value)
    return rmse_value, rmse_update


def compute_per_batch_eval_metrics(metrics, predictions, used_labels, labels, indices,
        used_indices, ori_batch_idx, step_idx, config, context):
    new_batch_idx = tf.gather(tf.range(tf.shape(used_indices)[0]), ori_batch_idx)
    new_indices = tf.concat([
        tf.expand_dims(new_batch_idx, 1),
        tf.expand_dims(step_idx, 1),
        tf.slice(indices,
                 begin=[0, 2],
                 size=[tf.shape(indices)[0], 1])],
        axis=1)
    label_shape = tf.shape(labels)
    eval_labels = tf.sparse_reshape(
        tf.SparseTensor(
            tf.cast(new_indices, tf.int64),
            tf.cast(used_labels, tf.int64),
            tf.cast(
                [tf.shape(used_indices)[0], label_shape[1], label_shape[2]],
                tf.int64)
            ),
        [tf.shape(used_indices)[0], label_shape[1] * label_shape[2]])
    ori_predictions = tf.gather(predictions, ori_batch_idx)
    updates = compute_recommendation_metrics(
            metrics, eval_labels, predictions)
    updates += compute_regression_metrics(
            metrics, ori_predictions, used_labels, indices, config, context)
    updates += compute_ranking_metrics(
            metrics, used_indices, new_batch_idx, used_labels, predictions, config)
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


def compute_recommendation_metrics(metrics, eval_labels, predictions):
    updates = []
    for metric in metrics.split(' '):
        if 'MAP' in metric:
            updates += compute_map_metrics(eval_labels, predictions, metric)[1]
        elif 'AP' in metric:
            updates += compute_ap_metrics(eval_labels, predictions, metric)[1]
        elif 'AR' in metric:
            updates += compute_ar_metrics(eval_labels, predictions, metric)[1]
    return updates


def compute_ranking_metrics(metrics, used_indices, new_batch_idx, used_labels, predictions, config):
    updates = []
    for metric in metrics.split(' '):
        if 'AUC' == metric:
            num_sampled = None
            if 'AUC' in config and 'num_sampled' in config['AUC']:
                num_sampled = config['AUC']['num_sampled']
            updates.append(compute_auc_metric(used_indices, new_batch_idx,
                used_labels, predictions, num_sampled=num_sampled)[1])
    return updates


def compute_regression_metrics(metrics, predictions, used_labels, indices, config, context):
    updates = []
    for metric in metrics.split(' '):
        if 'MAE' == metric:
            used_contexts = tf.gather_nd(context[config['MAE']['context']], indices)
            updates.append(compute_mae_metric(used_labels, used_contexts, predictions)[1])
        elif 'RMSE' == metric:
            used_contexts = tf.gather_nd(context[config['RMSE']['context']], indices)
            updates.append(compute_rmse_metric(used_labels, used_contexts, predictions)[1])
    return updates


def compute_per_step_eval_metrics(metrics, predictions, used_labels,
        indices, config, context):
    updates = compute_recommendation_metrics(
            metrics, tf.cast(used_labels, tf.int64), predictions)
    updates += compute_regression_metrics(
            metrics, predictions, used_labels, indices, config, context)
    return updates

