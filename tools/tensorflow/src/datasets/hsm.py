
import random

from src.dataset import DataSet

class HierarchicalSoftmaxSimulatedDataSet(DataSet):

    def __init__(self, config=None):
        if config is None:
            self._config = {
                'item': [
                    {
                        'attr': 'cluster',
                        'vocab_size': 10,
                        'softmax_dim': 10,
                        'embedding_dim': 10,
                    }, {
                        'attr': 'item',
                        'vocab_size': 20,
                        'softmax_dim': 10,
                        'item2cluster': [random.randint(0, 9) for _ in range(20)],
                        'embedding_dim': 10,
                    }
                ],
                'attr': {
                    'vocab_size': 10,
                    'embedding_dim': 10,
                }
            }
        else:
            self._config = config

    def next_batch(self):
        raise Exception('To be implemented.')
