
import unittest
import random

from src.trainer import ModelTrainer
from src.datasets.list_dataset import ListDataSet
from src.models.page_level_sequence import PageLevelSequenceModelBuilder

class PageLevelSequenceModelTest(unittest.TestCase):

    def setUp(self):
        #self._test_path = '/tmp/tflearn_logs/'
        self._test_path = '/opt/pyml/UserInaction/data/tensorboard/'

    def test_dump_graph(self):
        model_builder = PageLevelSequenceModelBuilder()
        model_builder.dump_graph(self._test_path + 'page_level_sequence_model.graph', 0.01)

    def test_build_model_with_trainer(self):
        model_builder = PageLevelSequenceModelBuilder(
                item_events=['display', 'click', 'high_rate', 'low_rate', 'wishlist'],
                predicted_event='click')
        item_events = {'click': 0.3, 'high_rate': 0.5, 'low_rate': 0.5, 'wishlist': 0.1}
        user_vocab_size = 10
        item_vocab_size = 10
        batches = []
        batch_size = 4
        page_size = 3
        for i in range(10):
            max_seq_len = random.randint(5, 10)
            batch = {'user_idx': [], 'display_idx': [], 'sequence_length_val': []}
            for l in range(batch_size):
                batch['user_idx'].append([random.randint(1, user_vocab_size - 1)])
                batch['sequence_length_val'].append([random.randint(1, max_seq_len)])
                batch['display_idx'].append([])
                for j in range(max_seq_len):
                    for k in range(page_size):
                        if j < batch['sequence_length_val'][l][0]:
                            batch['display_idx'][l].append(random.randint(1, item_vocab_size - 1))
                        else:
                            batch['display_idx'][l].append(0)
            for event, rate in item_events.iteritems():
                batch['%s_idx' % event] = []
                for l in range(batch_size):
                    batch['%s_idx' % event].append([])
                    for j in range(max_seq_len * page_size):
                        dice = random.random()
                        display = batch['display_idx'][l][j]
                        if display > 0 and dice < rate:
                            batch['%s_idx' % event][l].append(display)
                        else:
                            batch['%s_idx' % event][l].append(0)
            batches.append(batch)
        train_data = ListDataSet(batches)
        model_trainer = ModelTrainer(
            train_data, builder=model_builder, max_steps=10,
            tensorboard_dir=self._test_path)
        model_trainer.train('page_level_sequence_test_run0')

if __name__ == '__main__':
    unittest.main()
