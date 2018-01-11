
from ..dataset import DataSet

class ListDataSet(DataSet):

    def __init__(self, batches):
        self._batches = batches
        self._idx = 0

    def next_batch(self):
        while self._idx < len(self._batches):
            obj = self._batches[self._idx]
            feed_dict = {}
            for key, val in obj.iteritems():
                feed_dict['%s:0' % key] = val
            self._idx += 1
            yield feed_dict

    def reset(self):
        self._idx = 0
