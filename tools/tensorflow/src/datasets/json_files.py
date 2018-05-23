
import json
import logging

from src.dataset import DataSet

logger = logging.getLogger('json_files')


class JsonFilesDataSet(DataSet):

    def __init__(self, files=None, includes=None, excludes=None):
        if files is None:
            self._files = []
        else:
            self._files = files
        self._includes = includes
        if excludes is None:
            self._excludes = []
        else:
            self._excludes = excludes

    def next_batch(self):
        for afile in self._files:
            with open(afile) as fin:
                logger.info('Reading from %s.' % afile)
                for line in fin:
                    obj = json.loads(line.strip())
                    feed_dict = {}
                    for key, val in obj.iteritems():
                        if (self._includes is None or key in self._includes) and (key not in self._excludes):
                            feed_key = '%s:0' % key
                            feed_dict[feed_key] = val
                    yield feed_dict

    def reset(self):
        pass
