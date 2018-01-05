
class DataSet(object):

    def __init__(self):
        pass

    """A batch generator that generates a batch of data points in the same dict as feed_dict of TensorFlow."""
    def next_batch(self):
        feed_dict = {}
        yield feed_dict

    def reset(self):
        pass