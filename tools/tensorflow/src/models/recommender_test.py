
import unittest
import random

from src.trainer import ModelTrainer
from src.datasets.json_list import JsonListDataSet
from src.models.sequence_user_model import SequenceUserModel
from src.models.softmax_prediction_model import SoftmaxPredictionModel
from src.models.hsm_prediction_model import HierarchicalPredictionModel
from src.models.recommender import RecommenderBuilder


class RecommenderTest(unittest.TestCase):

    def setUp(self):
        self._test_path = '/tmp/tflearn_logs/'

    def test_sequence_hsm_model(self):
        embedding_dim = 10
        user_vocab_size = 15
        item_vocab_size = 20
        cluster_vocab_size = 7
        rnn_size = 5
        page_size = 3
        item2cluster = [random.randint(0, cluster_vocab_size-1) for _ in range(item_vocab_size)]
        user_model = SequenceUserModel(rnn_size)
        hsm_model = HierarchicalPredictionModel(hierarchies={
            'item': [
                {
                    'attr': 'cluster',
                    'vocab_size': cluster_vocab_size,
                    'softmax_dim': rnn_size
                }, {
                    'attr': 'item',
                    'vocab_size': item_vocab_size,
                    'item2cluster': item2cluster,
                    'softmax_dim': rnn_size
                }
            ]
        })
        model_builder = RecommenderBuilder(
            user_model, hsm_model,
            page_size=page_size,
            attr2config={
                'item': {
                    'vocab_size': item_vocab_size,
                    'embedding_dim': embedding_dim,
                    'is_numerical': False,
                    'level': 'item'
                },
                'user': {
                    'vocab_size': user_vocab_size,
                    'embedding_dim': embedding_dim,
                    'is_numerical': False,
                    'level': 'user'
                }
            },
            target2config={
                'item': {
                    'weight': 1.0
                }
            },
        )
        batches = []
        batch_size = 4
        for i in range(10):
            max_seq_len = random.randint(5, 10)
            batch = {'user_idx': [], 'item_idx': [], 'sequence_length_val': []}
            for l in range(batch_size):
                batch['user_idx'].append([random.randint(1, user_vocab_size - 1)])
                batch['sequence_length_val'].append([random.randint(1, max_seq_len)])
                batch['item_idx'].append([])
                for j in range(max_seq_len):
                    for k in range(page_size):
                        if j < batch['sequence_length_val'][l][0]:
                            batch['item_idx'][l].append(random.randint(1, item_vocab_size - 1))
                        else:
                            batch['item_idx'][l].append(0)
            batches.append(batch)
        train_data = JsonListDataSet(batches)
        model_trainer = ModelTrainer(
            train_data, builder=model_builder, max_steps=10,
            tensorboard_dir=self._test_path)
        model_trainer.train('recommender_test_sequence_hsm_model_run0')

    def test_sequence_softmax_model(self):
        embedding_dim = 10
        user_vocab_size = 15
        item_vocab_size = 20
        rnn_size = 5
        page_size = 3
        user_model = SequenceUserModel(rnn_size)
        softmax_model = SoftmaxPredictionModel(vocab_sizes={'item': item_vocab_size})
        model_builder = RecommenderBuilder(
            user_model, softmax_model,
            page_size=page_size,
            attr2config={
                'item': {
                    'vocab_size': item_vocab_size,
                    'embedding_dim': embedding_dim,
                    'is_numerical': False,
                    'level': 'item'
                },
                'user': {
                    'vocab_size': user_vocab_size,
                    'embedding_dim': embedding_dim,
                    'is_numerical': False,
                    'level': 'user'
                }
            },
            target2config={
                'item': {
                    'weight': 1.0
                }
            },
        )
        batches = []
        batch_size = 4
        for i in range(10):
            max_seq_len = random.randint(5, 10)
            batch = {'user_idx': [], 'item_idx': [], 'sequence_length_val': []}
            for l in range(batch_size):
                batch['user_idx'].append([random.randint(1, user_vocab_size - 1)])
                batch['sequence_length_val'].append([random.randint(1, max_seq_len)])
                batch['item_idx'].append([])
                for j in range(max_seq_len):
                    for k in range(page_size):
                        if j < batch['sequence_length_val'][l][0]:
                            batch['item_idx'][l].append(random.randint(1, item_vocab_size - 1))
                        else:
                            batch['item_idx'][l].append(0)
            batches.append(batch)
        train_data = JsonListDataSet(batches)
        model_trainer = ModelTrainer(
            train_data, builder=model_builder, max_steps=10,
            tensorboard_dir=self._test_path)
        model_trainer.train('recommender_test_sequence_softmax_model_run0')

if __name__ == '__main__':
    unittest.main()
