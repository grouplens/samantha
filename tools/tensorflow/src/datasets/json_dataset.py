
import json

from src.dataset import DataSet

class JsonDataSet(DataSet):

    def __init__(self, files=[]):
        self.files = files

    def next_batch(self):
        for afile in self.files:
            with open(afile) as fin:
                for line in fin:
                    obj = json.loads(line.strip())
                    feed_dict = {}
                    for key, val in obj.iteritems():
                        if key in ['display_idx', 'user_idx', 'sequence_length_val']:
                            feed_dict['%s:0' % key] = val
                    yield feed_dict

    def reset(self):
        pass
