
import unittest
import random

from src.trainer import ModelTrainer
from src.datasets.list_dataset import ListDataSet
from src.models.sequence_user_model import SequenceUserModel
from src.models.softmax_prediction_model import SoftmaxPredictionModel
from src.models.recommender import RecommenderBuilder


class RecommenderTest(unittest.TestCase):

    def setUp(self):
        self._test_path = '/tmp/tflearn_logs/'
        #self._test_path = '/opt/pyml/UserInaction/data/tensorboard/'

    def test_sequence_softmax_model(self):
        embedding_dim = 10
        user_vocab_size = 15
        item_vocab_size = 20
        rnn_size = 5
        page_size = 3
        user_model = SequenceUserModel(rnn_size)
        softmax_model = SoftmaxPredictionModel(item_vocab_size)
        model_builder = RecommenderBuilder(
            user_model, softmax_model,
            page_size=page_size,
            attr2config={
                'action': {
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
                'action': {
                    'weight': 1.0
                }
            },
        )
        batches = []
        batch_size = 4
        for i in range(10):
            max_seq_len = random.randint(5, 10)
            batch = {'user_idx': [], 'action_idx': [], 'sequence_length_val': []}
            for l in range(batch_size):
                batch['user_idx'].append([random.randint(1, user_vocab_size - 1)])
                batch['sequence_length_val'].append([random.randint(1, max_seq_len)])
                batch['action_idx'].append([])
                for j in range(max_seq_len):
                    for k in range(page_size):
                        if j < batch['sequence_length_val'][l][0]:
                            batch['action_idx'][l].append(random.randint(1, item_vocab_size - 1))
                        else:
                            batch['action_idx'][l].append(0)
            batches.append(batch)
        train_data = ListDataSet(batches)
        model_trainer = ModelTrainer(
            train_data, builder=model_builder, max_steps=10,
            tensorboard_dir=self._test_path)
        model_trainer.train('recommender_test_run0')

if __name__ == '__main__':
    unittest.main()
