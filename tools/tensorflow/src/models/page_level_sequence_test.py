
import unittest
import random

from ..trainer import ModelTrainer
from ..datasets.list_dataset import ListDataSet
from page_level_sequence import PageLevelSequenceModelBuilder

class PageLevelSequenceModelTest(unittest.TestCase):

    def setUp(self):
        pass

    def test_model_builder_with_trainer(self):
        model_builder = PageLevelSequenceModelBuilder()
        item_events = {'click': 0.3, 'high_rate': 0.5, 'low_rate': 0.5, 'wishlist': 0.1}
        user_vocab_size = 10000
        item_vocab_size = 10000
        batches = []
        batch_size = 128
        page_size = 24
        for i in range(10):
            max_seq_len = random.randint(50, 100)
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
        model_trainer = ModelTrainer(train_data, builder=model_builder)
        model_trainer.train('page_level_sequence_run1')

if __name__ == '__main__':
    unittest.main()
