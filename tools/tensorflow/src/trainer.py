
import random
import string

import tensorflow as tf

from builder import ModelBuilder

class ModelTrainer(object):

    def __init__(self,
                 train_data,
                 eval_data=None,
                 tensorboard_dir='/tmp/tflearn_logs/',
                 builder=ModelBuilder(),
                 max_steps=1e7,
                 learning_rate=0.01,
                 steps_per_eval=1000):
        self.train_data = train_data
        self.eval_data = eval_data
        self.tensorboard_dir = tensorboard_dir
        self.builder = builder
        self.max_steps = max_steps
        self.steps_per_eval = steps_per_eval
        self.learning_rate = learning_rate

    def train(self, run_name=None):
        session = tf.Session()
        with session.as_default():
            graph = tf.get_default_graph()
            with graph.as_default():
                loss = self.builder.build_model()
                update_op = tf.train.AdagradOptimizer(
                    self.learning_rate).minimize(loss, name='update_op')
                if run_name is None:
                    run_name = ''.join(random.choice(string.ascii_uppercase + string.digits) for _ in range(6))
                merged_summary = tf.summary.merge_all()
                train_writer = tf.summary.FileWriter(self.tensorboard_dir + run_name + '_train', graph)
                eval_writer = tf.summary.FileWriter(self.tensorboard_dir + run_name + '_eval', graph)
                step = 0
                while step < self.max_steps:
                    for train_batch in self.train_data.next_batch():
                        if step % self.steps_per_eval == 0:
                            if self.eval_data is not None:
                                for eval_batch in self.eval_data.next_batch():
                                    eval_summary = session.run(merged_summary, feed_dict=eval_batch)
                                    eval_writer.add_summary(eval_summary, step)
                                self.eval_data.reset()
                        train_summary, _ = session.run([merged_summary, update_op], feed_dict=train_batch)
                        train_writer.add_summary(train_summary, step)
                        step += 1
                        if step >= self.max_steps:
                            break
                    self.train_data.reset()
                train_writer.close()
                eval_writer.close()
        session.close()
