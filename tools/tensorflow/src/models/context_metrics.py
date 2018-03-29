
import tensorflow as tf


def compute_shown_auc_metric(eval_labels, predictions, indices, config, context):
    display = context[config['metric']['auc']['context']]
    used_display = tf.gather_nd(display, indices)
    batch_idx = tf.slice(
        indices,
        begin=[0, 0],
        size=[tf.shape(indices)[0], 1])
    preds_idx = tf.concat([batch_idx, tf.expand_dims(used_display, 1)], 1)
    eval_preds = tf.gather_nd(predictions, preds_idx)
    auc_value, auc_update = tf.metrics.auc(eval_labels > 0, eval_preds)
    tf.summary.scalar('ShownAUC', auc_value)
    return auc_value, auc_update


def compute_eval_label_metrics(metrics, predictions, labels, indices, config, context):
    all_labels = tf.gather_nd(labels, indices)
    updates = []
    for metric in metrics.split(' '):
        if 'ShownAUC' == metric:
            updates.append(compute_shown_auc_metric(all_labels, predictions, indices, config, context)[1])
    return updates

