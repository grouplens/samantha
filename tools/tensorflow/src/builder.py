
import tensorflow as tf

class ModelBuilder(object):

    def __init__(self):
        pass

    """ The method to construct a TensorFlow model in the default graph and return the loss to optimize.

    This is the interface for building a TensorFlow model and should be overridden.

    Returns:
        loss: A scalar Tensor indicating the loss of the model to optimize.
    """
    def build_model(self):
        raise Exception('This method must be overridden.')

    def test_tensors(self):
        return {}
