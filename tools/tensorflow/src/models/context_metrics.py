
import tensorflow as tf


def compute_shown_auc_metric(predictions, labels, indices, ori_batch_idx, config, context):
    display = context[config['ShownAUC']['context']]
    used_display = tf.gather_nd(display, indices)
    preds_idx = tf.concat([
        tf.expand_dims(ori_batch_idx, 1),
        tf.expand_dims(used_display, 1)], 1)
    eval_preds = tf.gather_nd(predictions, preds_idx)
    eval_labels = tf.gather_nd(labels, indices)
    mask = used_display > 0
    auc_value, auc_update = tf.metrics.auc(
        tf.boolean_mask(eval_labels > 0, mask),
        tf.boolean_mask(eval_preds, mask),
        num_thresholds=1000)
    tf.summary.scalar('ShownAUC', auc_value)
    return auc_value, auc_update


def compute_per_batch_eval_metrics(metrics, predictions, labels, indices, ori_batch_idx, config, context):
    updates = []
    for metric in metrics.split(' '):
        if 'ShownAUC' == metric:
            updates.append(compute_shown_auc_metric(predictions, labels, indices, ori_batch_idx, config, context)[1])
    return updates

