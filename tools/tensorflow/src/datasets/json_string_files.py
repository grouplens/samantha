
import json
import numpy as np
import logging

from src.dataset import DataSet

logger = logging.getLogger('json_string_files')

class JsonStringFilesDataSet(DataSet):

    def __init__(self, size_name, separator=',', files=None, includes=None, excludes=None):
        if files is None:
            self._files = []
        else:
            self._files = files
        self._includes = includes
        if excludes is None:
            self._excludes = []
        else:
            self._excludes = excludes
        self._size_name = size_name
        self._separator = separator

    def next_batch(self):
        for afile in self._files:
            with open(afile) as fin:
                logger.info('Reading from %s.' % afile)
                for line in fin:
                    obj = json.loads(line.strip())
                    sizes = [int(float(x)) for x in obj[self._size_name].split(self._separator)]
                    max_size = max(sizes)
                    sum_size = sum(sizes)
                    feed_dict = {}
                    if self._includes is None:
                        names = obj.keys()
                    else:
                        names = self._includes
                    for key in names:
                        if key in self._excludes:
                            continue
                        val = obj[key]
                        if '_idx' in key:
                            vals = [int(x) for x in val.split(self._separator)]
                            default = 0
                        else:
                            vals = [float(x) for x in val.split(self._separator)]
                            default = 1.0
                        logger.info("%s max_size: %s, sum_size: %s, seq_len: %s.",
                                key, max_size, sum_size, len(vals))
                        logger.info("%s seq_len / 24 = %s", key, len(vals) / 24.0)
                        if len(vals) == sum_size:
                            batch = []
                            start = 0
                            for size in sizes:
                                ins = vals[start:(start + size)] + [default] * (max_size - size)
                                batch.append(ins)
                                start += size
                        else:
                            batch = [[x] for x in vals]
                        np_batch = np.array(batch)
                        logger.info("%s batch.shape: %s", key, np_batch.shape)
                        feed_dict['%s:0' % key] = np_batch
                    yield feed_dict

    def reset(self):
        pass
