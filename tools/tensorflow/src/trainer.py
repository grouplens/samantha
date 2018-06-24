
# from tensorflow.python import debug as tf_debug

import os
import random
import string
import logging

import tensorflow as tf

from src.builder import ModelBuilder

logger = logging.getLogger('trainer')

class ModelTrainer(object):

    def __init__(self,
                 train_data,
                 tensorboard_dir='/tmp/tflearn_logs/',
                 export_dir='/tmp/tflearn_models/',
                 builder=ModelBuilder(),
                 max_steps=1e7,
                 export_per_steps=1e4,
                 max_epochs=100,
                 export_per_epochs=10,
                 learning_rate=0.01,
                 early_stop_by='epoch',
                 step_epsilon=0.0,
                 epoch_epsilon=0.0):
        self._train_data = train_data
        self._tensorboard_dir = tensorboard_dir
        self._export_dir = export_dir
        self._builder = builder
        self._max_steps = max_steps
        self._export_per_steps = export_per_steps
        self._learning_rate = learning_rate
        self._step_eval_losses = []
        self._epoch_eval_losses = []
        self._max_epochs = max_epochs
        self._export_per_epochs = export_per_epochs
        self._early_stop_by = early_stop_by  # could be 'epoch' or 'step' or 'both' or None
        self._step_epsilon = step_epsilon
        self._epoch_epsilon = epoch_epsilon

    def _export_model(self, session, folder):
        builder = tf.saved_model.builder.SavedModelBuilder(folder)
        builder.add_meta_graph_and_variables(session, ['train_eval_serve'])
        builder.save()

    def _whether_early_stop(self, losses, epsilon, trace_back=2):
        if len(losses) < trace_back or losses[-trace_back] - losses[-1] >= epsilon:
            return False
        return True

    def train(self, run_name=None):
        graph = tf.Graph()
        with graph.as_default():
            session = tf.Session(graph=graph)
            # session = tf_debug.LocalCLIDebugWrapperSession(session)
            with session.as_default():
                logger.info('Building the model graph.')
                train_loss, eval_loss, updates = self._builder.build_model()
                update_op = self._builder.build_optimizer(train_loss, self._learning_rate)
                for update in updates:
                    update_op = tf.group(update_op, update)
                merged_summary = tf.summary.merge_all()
                run_tensors = {
                    'merged_summary': merged_summary,
                    'update_op': update_op,
                    'train_loss': train_loss,
                    'eval_loss': eval_loss,
                }
                if run_name is None:
                    run_name = ''.join(
                        random.choice(string.ascii_uppercase + string.digits) for _ in range(6))
                train_writer = tf.summary.FileWriter(os.path.join(self._tensorboard_dir, run_name), graph)
                logger.info('Initializing the model graph.')
                session.run([tf.global_variables_initializer(), tf.local_variables_initializer()])
                logger.info('Training the model.')
                step = 0
                epoch = 0
                early_stopped = False
                while step < self._max_steps and epoch < self._max_epochs:
                    epoch_train_loss = 0.0
                    epoch_eval_loss = 0.0
                    for train_batch in self._train_data.next_batch():
                        train_vals = session.run(run_tensors, feed_dict=train_batch)
                        step += 1
                        train_writer.add_summary(train_vals['merged_summary'], step)
                        logger.info('Step %s, training loss: %s, evaluation loss: %s',
                                    step, train_vals['train_loss'], train_vals['eval_loss'])
                        epoch_train_loss += train_vals['train_loss']
                        epoch_eval_loss += train_vals['eval_loss']
                        self._step_eval_losses.append(train_vals['eval_loss'])
                        if step % self._export_per_steps == 0:
                            self._export_model(session, os.path.join(self._export_dir, 'step_%s' % step))
                        if step >= self._max_steps:
                            break
                        if self._early_stop_by in ['step', 'both'] and self._whether_early_stop(
                                self._step_eval_losses, self._step_epsilon):
                            early_stopped = True
                            break
                    self._train_data.reset()
                    epoch += 1
                    logger.info('Epoch %s, training loss: %s, evaluation loss: %s',
                                epoch, epoch_train_loss, epoch_eval_loss)
                    self._epoch_eval_losses.append(epoch_eval_loss)
                    if epoch % self._export_per_epochs == 0:
                        self._export_model(session, os.path.join(self._export_dir, 'epoch_%s' % epoch))
                    if early_stopped or (self._early_stop_by in ['epoch', 'both'] and self._whether_early_stop(
                            self._epoch_eval_losses, self._epoch_epsilon)):
                        early_stopped = True
                        break
                if early_stopped:
                    logger.info("Early stopped because of non-decreasing evaluation loss")
                logger.info("Stopped at epoch %s, step %s." % (epoch, step))
                train_writer.close()
                self._export_model(session, os.path.join(self._export_dir, 'final'))
            session.close()
