
import random
import logging
import os
import json
import numpy as np

from src.dataset import DataSet
from src.datasets.json_files import JsonFilesDataSet

logger = logging.getLogger('simulated')


class SVDSoftmaxSimulatedDataSet(DataSet):

    def __init__(self, config=None):
        if config is None:
            self._config = {
                'embedding_dim': 5,
                'user_vocab': 5,
                'item_vocab': 100,
                'attr_vocab': 10,
                'max_seq_len': 10,
                'min_seq_len': 2,
                'batch_size': 4,
                'page_size': 1,
                'hierarchical': False,
                'json_file': 'json_file.json',
            }
        else:
            self._config = config
        self._user_idx = 0
        self._item2attr = None
        self._embeddings = {}
        if os.path.exists(self._config['json_file']):
            self._json_file_dataset = JsonFilesDataSet(files=[self._config['json_file']])
        else:
            self._json_file_dataset = None
            self._embeddings = {
                'user': np.random.rand(self._config['user_vocab'], self._config['embedding_dim']),
                'item': np.random.rand(self._config['item_vocab'], self._config['embedding_dim']),
                'attr': np.random.rand(self._config['attr_vocab'], self._config['embedding_dim']),
            }
            self._item2attr, self._attr2items = self._generate_item_attr()

    def _generate_item_attr(self):
        item2attr = [random.randint(0, self._config['attr_vocab'] - 1) for _ in range(
            self._config['item_vocab'])]
        attr2items = []
        return item2attr, attr2items

    def _generate_from_full_softmax(self, input, weights, size):
        logits = np.matmul(input, np.transpose(weights))
        exp_logits = np.exp(logits)
        probs = np.divide(exp_logits, np.sum(exp_logits))
        dices = np.random.choice(range(len(probs)), size=[size], p=probs)
        return dices

    def _generate_from_partial_softmax(self, input, weights, includes):
        part_weights = []
        for idx in includes:
            part_weights.append(weights[idx])
        logits = np.matmul(input, np.transpose(part_weights))
        exp_logits = np.exp(logits)
        probs = np.divide(exp_logits, np.sum(exp_logits))
        dices = np.random.choice(range(len(probs)), size=[1], p=probs)
        return dices[0]

    def _generate_item_attr(self, user_idx, size):
        if self._config['hierarchical']:
            attr_indices = self._generate_from_full_softmax(
                self._embeddings['user'][user_idx],
                self._embeddings['attr'], size)
            item_indices = []
            for attr_idx in attr_indices:
                item_idx = self._generate_from_sub_softmax(
                    self._embeddings['attr'][attr_idx],
                    self._embeddings['item'], self._attr2items[attr_idx])
                item_indices.append(item_idx)
        else:
            item_indices = self._generate_from_full_softmax(
                self._embeddings['user'][user_idx],
                self._embeddings['item'], size)
            attr_indices = []
            for item_idx in item_indices:
                attr_indices.append(self._item2attr[item_idx])
        return item_indices, attr_indices

    def next_batch(self):
        if self._json_file_dataset is None:
            with open(self._config['json_file'], 'w') as fout:
                num_batches = self._config['user_vocab'] / self._config['batch_size']
                for i in range(num_batches):
                    seq_len = random.randint(self._config['min_seq_len'], self._config['max_seq_len'])
                    seq_len *= self._config['page_size']
                    batch = {'user_idx': [], 'item_idx': [], 'sequence_length_val': [], 'attr_idx': []}
                    for l in range(self._config['batch_size']):
                        self._user_idx += 1
                        batch['user_idx'].append([self._user_idx])
                        batch['sequence_length_val'].append([seq_len])
                        item_indices, attr_indices = self._generate_item_attr(self._user_idx, seq_len)
                        batch['item_idx'].append(item_indices)
                        batch['attr_idx'].append(attr_indices)
                    json.dump(batch, fout)
                    yield batch
        else:
            yield self._json_file_dataset.next_batch()

    def reset(self):
        if os.path.exists(self._config['json_file']):
            self._json_file_dataset = JsonFilesDataSet(files=[self._config['json_file']])
        self._user_idx = 0
