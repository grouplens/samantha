
import tensorflow as tf


class ModelBuilder(object):

    def __init__(self):
        pass

    """ The method to construct a TensorFlow model in the default graph and return the loss to optimize.

    This is the interface for building a TensorFlow model and should be overridden.

    Returns:
        train_loss: A scalar Tensor with the loss of the model to optimize.
        eval_loss: A scalar Tensor with the testing loss of the batch, used by the trainer to decide on early stopping.
        updates: A list of update operations for computing metrics. Can be empty.
    """
    def build_model(self):
        raise Exception('This method must be overridden.')

    def test_tensors(self):
        return {}

    def build_optimizer(self, loss, learning_rate):
        return tf.train.AdagradOptimizer(learning_rate).minimize(loss, name='update_op')

    def dump_graph(self, file_path, learning_rate):
        graph = tf.Graph()
        with graph.as_default():
            loss, _ = self.build_model()
            self.build_optimizer(loss, learning_rate)
            tf.group(
                tf.global_variables_initializer(), tf.local_variables_initializer(),
                name='init_op'
            )
            with open(file_path, 'w') as fout:
                fout.write(graph.as_graph_def().SerializeToString())
