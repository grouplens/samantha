
import random
import string
import logging

import tensorflow as tf

from builder import ModelBuilder

class ModelTrainer(object):

    def __init__(self,
                 train_data,
                 tensorboard_dir='/tmp/tflearn_logs/',
                 builder=ModelBuilder(),
                 max_steps=1e7,
                 learning_rate=0.01):
        self.train_data = train_data
        self.tensorboard_dir = tensorboard_dir
        self.builder = builder
        self.max_steps = max_steps
        self.learning_rate = learning_rate

    def train(self, run_name=None):
        session = tf.Session()
        with session.as_default():
            graph = tf.get_default_graph()
            with graph.as_default():
                logging.info('Building the model graph.')
                print 'Building the model graph.'
                loss, updates = self.builder.build_model()
                run_tensors = self.builder.test_tensors()
                update_op = tf.train.AdagradOptimizer(self.learning_rate).minimize(loss)
                for update in updates:
                    update_op = tf.group(update_op, update)
                merged_summary = tf.summary.merge_all()
                run_tensors['merged_summary'] = merged_summary
                run_tensors['update_op'] = update_op
                run_tensors['train_loss'] = loss
                if run_name is None:
                    run_name = ''.join(
                        random.choice(string.ascii_uppercase + string.digits) for _ in range(6))
                train_writer = tf.summary.FileWriter(self.tensorboard_dir + run_name, graph)
                logging.info('Initializing the model graph.')
                print 'Initializing the model graph.'
                session.run([tf.global_variables_initializer(), tf.local_variables_initializer()])
                logging.info('Training the model.')
                print 'Training the model.'
                step = 0
                while step < self.max_steps:
                    for train_batch in self.train_data.next_batch():
                        train_vals = session.run(run_tensors, feed_dict=train_batch)
                        train_writer.add_summary(train_vals['merged_summary'], step)
                        step += 1
                        logging.info('Step %s, training loss: %s', step, train_vals['train_loss'])
                        print 'Step %s, training loss: %s' % (step, train_vals['train_loss'])
                        if step >= self.max_steps:
                            break
                    self.train_data.reset()
                train_writer.close()
        session.close()
