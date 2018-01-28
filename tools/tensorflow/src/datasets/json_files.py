
import json
import numpy as np
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
                    if self._includes is None:
                        for key, val in obj.iteritems():
                            if key not in self._excludes:
                                feed_dict['%s:0' % key] = np.array(val)
                    else:
                        for key in self._includes:
                            if key not in self._excludes:
                                feed_dict['%s:0' % key] = np.array(obj[key])
                    yield feed_dict

    def reset(self):
        pass
