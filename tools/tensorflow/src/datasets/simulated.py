
import random
import logging

import tensorflow as tf

from src.dataset import DataSet
from src.models.sequence_user_model import SequenceUserModel
from src.models.svdpp_user_model import SVDPPUserModel
from src.models.softmax_prediction_model import SoftmaxPredictionModel
from src.models.hsm_prediction_model import HierarchicalPredictionModel
from src.models.recommender import RecommenderBuilder

logger = logging.getLogger('simulated')


class SimulatedRecommenderDataSet(DataSet):

    def __init__(self, config=None):
        if config is None:
            self._config = {
                'user_side': 'seq',
                'hierarchy': 'flat',
                'softmax_dim': 32,
                'embedding_dim': 32,
                'user_vocab_size': 1,
                'item_vocab_size': 100,
                'attr_vocab_size': 10,
                'item2cluster_skewness': 0.5,
                'max_seq_len': 150,
                'seq_len_skewness': 0.5,
                'batch_size': 32,
            }
        else:
            self._config = config
        self._build_recommender()
        self._start_session()

    def _generate_item2cluster(self):
        return [random.randint(0, self._config['attr_vocab_size'] - 1) for _ in range(
            self._config['item_vocab_size'])]

    def _build_recommender(self):
        user_side = self._config['user_side']
        hierarchy = self._config['hierarchy']
        page_size = 1
        if 'page_size' in self._config:
            page_size = self._config['page_size']
        rnn_size = 32
        if 'rnn_size' in self._config:
            rnn_size = self._config['rnn_size']
        softmax_dim = self._config['softmax_dim']
        embedding_dim = self._config['embedding_dim']
        user_vocab_size = self._config['user_vocab_size']
        item_vocab_size = self._config['item_vocab_size']
        attr_vocab_size = self._config['attr_vocab_size']

        if user_side == 'seq':
            user_model = SequenceUserModel(rnn_size)
        elif user_side == 'svd':
            user_model = SVDPPUserModel(user_attr='user', item_attrs=['item', 'attr'])
        else:
            raise Exception('User model %s is not supported.' % user_side)

        target2config = {
            'item': {
                'weight': 1.0
            }
        }
        if hierarchy == 'flat':
            softmax_model = SoftmaxPredictionModel(config={
                'item': {
                    'vocab_size': item_vocab_size,
                    'softmax_dim': softmax_dim,
                }
            })
        elif hierarchy == 'rattr':
            config = {
                'item': {
                    'vocab_size': item_vocab_size,
                    'softmax_dim': softmax_dim,
                    'attrs': {
                        'attr': self._generate_item2cluster()
                    }
                },
                'attr': {
                    'vocab_size': attr_vocab_size,
                    'softmax_dim': softmax_dim,
                }
            }
            softmax_model = SoftmaxPredictionModel(config=config)
        elif hierarchy == 'attr':
            hierarchies = {
                'item': [
                    {
                        'attr': 'brand',
                        'vocab_size': attr_vocab_size,
                        'softmax_dim': softmax_dim,
                    }, {
                        'attr': 'item',
                        'vocab_size': item_vocab_size,
                        'softmax_dim': softmax_dim,
                        'item2cluster': self._generate_item2cluster()
                    }
                ]
            }
            softmax_model = HierarchicalPredictionModel(hierarchies=hierarchies)
        else:
            raise Exception('hierarchy %s is not supported.' % hierarchy)

        attr2config = {
            'item': {
                'vocab_size': item_vocab_size,
                'embedding_dim': embedding_dim,
                'is_numerical': False,
                'level': 'item'
            },
            'attr': {
                'vocab_size': attr_vocab_size,
                'embedding_dim': embedding_dim,
                'is_numerical': False,
                'level': 'item'
            },
            'user': {
                'vocab_size': user_vocab_size,
                'embedding_dim': embedding_dim,
                'is_numerical': False,
                'level': 'user'
            },
        }

        embedding_attrs = ['item', 'attr']
        if user_side == 'svd':
            embedding_attrs.append('user')

        self._builder = RecommenderBuilder(
            user_model=user_model,
            prediction_model=softmax_model,
            page_size=page_size,
            attr2config=attr2config,
            embedding_attrs=embedding_attrs,
            target2config=target2config,
        )

    def _start_session(self):
        graph = tf.Graph()
        with graph.as_default():
            self._session = tf.Session(graph=graph)
            with self._session.as_default():
                logger.info('Building the model graph.')
                self._builder.build_model()

    def next_batch(self):
        # generated the seq lens of the batch
        # get the biggest seq len
        # for i=0 to biggest_seq_len
        # # initially set to zero
        # # filter by i vs. seq len
        # # get prediction
        # # sample item by predictions
        # # attach attr
        # # set the filtered to be the sampled item and attr
        # pack the batch and return
        raise Exception('To be implemented.')
