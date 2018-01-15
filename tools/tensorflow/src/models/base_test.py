
import unittest
import random

from src.trainer import ModelTrainer
from src.datasets.list_dataset import ListDataSet
from src.models.base import BaseModelBuilder


class BaseModelTest(unittest.TestCase):

    def setUp(self):
        # self._test_path = '/tmp/tflearn_logs/'
        self._test_path = '/opt/pyml/UserInaction/data/tensorboard/'

    def test_dump_graph(self):
        model_builder = BaseModelBuilder()
        model_builder.dump_graph(self._test_path + 'base_model.graph', 0.01)

    def test_build_model_with_trainer(self):
        embedding_dim = 10
        user_vocab_size = 10
        item_vocab_size = 10
        page_size = 3
        model_builder = BaseModelBuilder(
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
            targets={
                'action': {
                    'weight': 1.0
                }
            },
            page_size=page_size,
            rnn_size=embedding_dim,
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
        model_trainer.train('base_test_run0')

if __name__ == '__main__':
    unittest.main()
