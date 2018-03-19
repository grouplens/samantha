
from tensorflow.python import debug as tf_debug

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
                 learning_rate=0.01):
        self._train_data = train_data
        self._tensorboard_dir = tensorboard_dir
        self._export_dir = export_dir
        self._builder = builder
        self._max_steps = max_steps
        self._learning_rate = learning_rate

    def train(self, run_name=None):
        graph = tf.Graph()
        with graph.as_default():
            session = tf.Session(graph=graph)
            # session = tf_debug.LocalCLIDebugWrapperSession(session)
            with session.as_default():
                logger.info('Building the model graph.')
                loss, updates = self._builder.build_model()
                update_op = self._builder.build_optimizer(loss, self._learning_rate)
                for update in updates:
                    update_op = tf.group(update_op, update)
                merged_summary = tf.summary.merge_all()
                run_tensors = {
                    'merged_summary': merged_summary,
                    'update_op': update_op,
                    'train_loss': loss
                }
                if run_name is None:
                    run_name = ''.join(
                        random.choice(string.ascii_uppercase + string.digits) for _ in range(6))
                train_writer = tf.summary.FileWriter(self._tensorboard_dir + run_name, graph)
                logger.info('Initializing the model graph.')
                session.run([tf.global_variables_initializer(), tf.local_variables_initializer()])
                logger.info('Training the model.')
                step = 0
                while step < self._max_steps:
                    for train_batch in self._train_data.next_batch():
                        train_vals = session.run(run_tensors, feed_dict=train_batch)
                        train_writer.add_summary(train_vals['merged_summary'], step)
                        step += 1
                        logger.info('Step %s, training loss: %s', step, train_vals['train_loss'])
                        if step >= self._max_steps:
                            break
                    self._train_data.reset()
                train_writer.close()
            builder = tf.saved_model.builder.SavedModelBuilder(self._export_dir)
            builder.add_meta_graph_and_variables(session, 'train_eval_serve')
            builder.save()
            session.close()
