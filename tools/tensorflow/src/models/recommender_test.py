
import unittest
import random
import shutil
import numpy as np
import tensorflow as tf

from src.trainer import ModelTrainer
from src.datasets.json_list import JsonListDataSet
from src.models.sequence_user_model import SequenceUserModel
from src.models.svdpp_user_model import SVDPPUserModel
from src.models.softmax_prediction_model import SoftmaxPredictionModel
from src.models.logistic_prediction_model import LogisticPredictionModel
from src.models.ccf_prediction_model import CCFSoftmaxModel
from src.models.bpr_prediction_model import BPRPredictionModel
from src.models.ctr_prediction_model import CTRPredictionModel
from src.models.regression_prediction_model import RegressionPredictionModel
from src.models.hsm_prediction_model import HierarchicalPredictionModel
from src.models.recommender import RecommenderBuilder


class RecommenderTest(unittest.TestCase):

    def setUp(self):
        self._test_path = '/tmp/tflearn_logs/'
        self._export_dir = '/tmp/tflearn_models/'

    def test_run_seq_flat_default_eval(self):
        embedding_dim = 10
        item_vocab_size = 20
        rnn_size = 5
        user_model = SequenceUserModel(rnn_size)
        softmax_model = SoftmaxPredictionModel(config={
            'item': {'vocab_size': item_vocab_size, 'softmax_dim': rnn_size}})
        model_builder = RecommenderBuilder(
            user_model, softmax_model,
            attr2config={
                'item': {
                    'vocab_size': item_vocab_size,
                    'embedding_dim': embedding_dim,
                    'is_numerical': False,
                    'level': 'item'
                },
            },
            target2config={
                'item': {
                    'weight': 1.0
                }
            },
            max_train_steps=7,
            train_steps=1,
            eval_steps=1,
        )
        graph = tf.Graph()
        with graph.as_default():
            session = tf.Session(graph=graph)
            with session.as_default():
                model_builder.build_model()
                run_tensors = model_builder.test_tensors()
                tensor_vals = session.run(run_tensors, feed_dict={
                    'item_idx:0': [[4, 9, 10, 14, 0, 0, 0], [2, 3, 11, 2, 4, 9, 8]],
                    'sequence_length_val:0': [[4], [7]]
                })
                np.testing.assert_array_equal(tensor_vals['train_indices'], np.array([
                    [0, 2, 0], [1, 5, 0],
                ]))
                np.testing.assert_array_equal(tensor_vals['eval_indices'], np.array([
                    [0, 3, 0], [1, 6, 0],
                ]))

    def test_run_seq_flat_tstamp_eval(self):
        embedding_dim = 10
        item_vocab_size = 20
        rnn_size = 5
        user_model = SequenceUserModel(rnn_size)
        softmax_model = SoftmaxPredictionModel(config={
            'item': {'vocab_size': item_vocab_size, 'softmax_dim': rnn_size}})
        model_builder = RecommenderBuilder(
            user_model, softmax_model,
            attr2config={
                'item': {
                    'vocab_size': item_vocab_size,
                    'embedding_dim': embedding_dim,
                    'is_numerical': False,
                    'level': 'item'
                },
                'tstamp': {
                    'is_numerical': True,
                    'level': 'item'
                }
            },
            embedding_attrs=['item'],
            target2config={
                'item': {
                    'weight': 1.0
                }
            },
            split_tstamp=10,
            tstamp_attr='tstamp',
            max_train_steps=7,
            train_steps=2,
            eval_steps=1,
        )
        graph = tf.Graph()
        with graph.as_default():
            session = tf.Session(graph=graph)
            with session.as_default():
                model_builder.build_model()
                run_tensors = model_builder.test_tensors()
                tensor_vals = session.run(run_tensors, feed_dict={
                    'item_idx:0': [[4, 9, 10, 14, 0, 0, 0], [2, 3, 11, 2, 4, 9, 8]],
                    'sequence_length_val:0': [[4], [7]],
                    'tstamp_val:0': [[2, 10, 12, 18, 0, 0, 0], [1, 2, 8, 9, 10, 20, 29]]
                })
                np.testing.assert_array_equal(tensor_vals['train_indices'], np.array([
                    [0, 0, 0], [1, 2, 0], [1, 3, 0],
                ]))
                np.testing.assert_array_equal(tensor_vals['eval_indices'], np.array([
                    [0, 1, 0], [1, 4, 0],
                ]))

    def test_train_sequence_hsm_model(self):
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
                    'softmax_dim': rnn_size,
                    'num_sampled': 5,
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
                batch['sequence_length_val'].append([random.randint(2, max_seq_len) * page_size])
                batch['item_idx'].append([])
                for j in range(max_seq_len):
                    for k in range(page_size):
                        if j < batch['sequence_length_val'][l][0] / page_size:
                            batch['item_idx'][l].append(random.randint(1, item_vocab_size - 1))
                        else:
                            batch['item_idx'][l].append(0)
            batches.append(batch)
        train_data = JsonListDataSet(batches)
        run_name = 'test_sequence_hsm_model_run0'
        export_dir = self._export_dir + run_name
        shutil.rmtree(export_dir, ignore_errors=True)
        model_trainer = ModelTrainer(
            train_data, builder=model_builder, max_steps=10,
            tensorboard_dir=self._test_path,
            export_dir=export_dir)
        model_trainer.train(run_name)

    def test_train_sequence_softmax_model(self):
        embedding_dim = 10
        user_vocab_size = 15
        item_vocab_size = 20
        rnn_size = 5
        page_size = 3
        user_model = SequenceUserModel(rnn_size)
        softmax_model = SoftmaxPredictionModel(config={
            'item': {'vocab_size': item_vocab_size, 'softmax_dim': rnn_size}})
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
                batch['sequence_length_val'].append([random.randint(2, max_seq_len) * page_size])
                batch['item_idx'].append([])
                for j in range(max_seq_len):
                    for k in range(page_size):
                        if j < batch['sequence_length_val'][l][0] / page_size:
                            batch['item_idx'][l].append(random.randint(1, item_vocab_size - 1))
                        else:
                            batch['item_idx'][l].append(0)
            batches.append(batch)
        train_data = JsonListDataSet(batches)
        run_name = 'test_sequence_softmax_model_run0'
        export_dir = self._export_dir + run_name
        shutil.rmtree(export_dir, ignore_errors=True)
        model_trainer = ModelTrainer(
            train_data, builder=model_builder, max_steps=10,
            tensorboard_dir=self._test_path,
            export_dir=export_dir)
        model_trainer.train(run_name)

    def test_train_ccf_softmax_model(self):
        user_vocab_size = 15
        item_vocab_size = 20
        embedding_dim = 5
        softmax_dim = embedding_dim
        page_size = 3
        user_model = SVDPPUserModel(item_attrs=[])
        softmax_model = CCFSoftmaxModel('user', user_vocab_size, 'display', page_size, config={
            'item': {'vocab_size': item_vocab_size, 'softmax_dim': softmax_dim}})
        model_builder = RecommenderBuilder(
            user_model, softmax_model,
            page_size=page_size,
            eval_metrics='MAP@1,5 AUC ShownAUC AP@1,5 AR@1,5',
            attr2config={
                'display': {
                    'vocab_size': item_vocab_size,
                    'embedding_dim': embedding_dim,
                    'is_numerical': False,
                    'level': 'item'
                },
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
            embedding_attrs=['user'],
            target2config={
                'item': {
                    'weight': 1.0,
                    'metric': {
                        'auc': {
                            'context': 'display'
                        }
                    }
                }
            },
        )
        batches = []
        batch_size = 4
        for i in range(10):
            max_seq_len = random.randint(5, 10)
            batch = {'user_idx': [], 'item_idx': [], 'display_idx': [], 'sequence_length_val': []}
            for l in range(batch_size):
                batch['user_idx'].append([random.randint(1, user_vocab_size - 1)])
                batch['sequence_length_val'].append([random.randint(2, max_seq_len) * page_size])
                batch['item_idx'].append([])
                batch['display_idx'].append([])
                for j in range(max_seq_len):
                    for k in range(page_size):
                        if j < batch['sequence_length_val'][l][0] / page_size:
                            idx = random.randint(1, item_vocab_size - 1)
                            batch['display_idx'][l].append(idx)
                            act_dice = random.random()
                            if act_dice < 0.01:
                                batch['item_idx'][l].append(idx)
                            else:
                                batch['item_idx'][l].append(0)
                        else:
                            batch['display_idx'][l].append(0)
                            batch['item_idx'][l].append(0)
            batches.append(batch)
        train_data = JsonListDataSet(batches)
        run_name = 'test_ccf_softmax_model_run0'
        export_dir = self._export_dir + run_name
        shutil.rmtree(export_dir, ignore_errors=True)
        model_trainer = ModelTrainer(
            train_data, builder=model_builder, max_steps=10,
            tensorboard_dir=self._test_path,
            export_dir=export_dir)
        model_trainer.train(run_name)

    def test_train_bpr_model(self):
        user_vocab_size = 15
        item_vocab_size = 20
        embedding_dim = 5
        sigmoid_dim = embedding_dim
        page_size = 3
        user_model = SVDPPUserModel(item_attrs=[])
        sigmoid_model = BPRPredictionModel('display', page_size, config={
            'item': {'vocab_size': item_vocab_size, 'sigmoid_dim': sigmoid_dim}})
        model_builder = RecommenderBuilder(
            user_model, sigmoid_model,
            page_size=page_size,
            attr2config={
                'display': {
                    'vocab_size': item_vocab_size,
                    'embedding_dim': embedding_dim,
                    'is_numerical': False,
                    'level': 'item'
                },
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
            embedding_attrs=['user'],
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
            batch = {'user_idx': [], 'item_idx': [], 'display_idx': [], 'sequence_length_val': []}
            for l in range(batch_size):
                batch['user_idx'].append([random.randint(1, user_vocab_size - 1)])
                batch['sequence_length_val'].append([random.randint(2, max_seq_len) * page_size])
                batch['item_idx'].append([])
                batch['display_idx'].append([])
                for j in range(max_seq_len):
                    for k in range(page_size):
                        if j < batch['sequence_length_val'][l][0] / page_size:
                            idx = random.randint(1, item_vocab_size - 1)
                            batch['display_idx'][l].append(idx)
                            act_dice = random.random()
                            if act_dice < 0.5:
                                batch['item_idx'][l].append(idx)
                            else:
                                batch['item_idx'][l].append(0)
                        else:
                            batch['display_idx'][l].append(0)
                            batch['item_idx'][l].append(0)
            batches.append(batch)
        train_data = JsonListDataSet(batches)
        run_name = 'test_bpr_sigmoid_model_run0'
        export_dir = self._export_dir + run_name
        shutil.rmtree(export_dir, ignore_errors=True)
        model_trainer = ModelTrainer(
            train_data, builder=model_builder, max_steps=10,
            tensorboard_dir=self._test_path,
            export_dir=export_dir)
        model_trainer.train(run_name)

    def test_train_ctr_model(self):
        user_vocab_size = 15
        item_vocab_size = 20
        embedding_dim = 5
        sigmoid_dim = embedding_dim
        page_size = 3
        user_model = SVDPPUserModel(item_attrs=[])
        sigmoid_model = CTRPredictionModel('display', config={
            'item': {'vocab_size': item_vocab_size, 'sigmoid_dim': sigmoid_dim}})
        model_builder = RecommenderBuilder(
            user_model, sigmoid_model,
            page_size=page_size,
            attr2config={
                'display': {
                    'vocab_size': item_vocab_size,
                    'embedding_dim': embedding_dim,
                    'is_numerical': False,
                    'level': 'item'
                },
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
            embedding_attrs=['user'],
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
            batch = {'user_idx': [], 'item_idx': [], 'display_idx': [], 'sequence_length_val': []}
            for l in range(batch_size):
                batch['user_idx'].append([random.randint(1, user_vocab_size - 1)])
                batch['sequence_length_val'].append([random.randint(2, max_seq_len) * page_size])
                batch['item_idx'].append([])
                batch['display_idx'].append([])
                for j in range(max_seq_len):
                    for k in range(page_size):
                        if j < batch['sequence_length_val'][l][0] / page_size:
                            idx = random.randint(1, item_vocab_size - 1)
                            batch['display_idx'][l].append(idx)
                            act_dice = random.random()
                            if act_dice < 0.5:
                                batch['item_idx'][l].append(idx)
                            else:
                                batch['item_idx'][l].append(0)
                        else:
                            batch['display_idx'][l].append(0)
                            batch['item_idx'][l].append(0)
            batches.append(batch)
        train_data = JsonListDataSet(batches)
        run_name = 'test_ctr_sigmoid_model_run0'
        export_dir = self._export_dir + run_name
        shutil.rmtree(export_dir, ignore_errors=True)
        model_trainer = ModelTrainer(
            train_data, builder=model_builder, max_steps=10,
            tensorboard_dir=self._test_path,
            export_dir=export_dir)
        model_trainer.train(run_name)

    def test_train_sequence_logistic_model(self):
        embedding_dim = 10
        user_vocab_size = 15
        item_vocab_size = 20
        rnn_size = 5
        page_size = 3
        user_model = SequenceUserModel(rnn_size)
        logistic_model = LogisticPredictionModel(config={
            'item': {'vocab_size': item_vocab_size, 'logistic_dim': rnn_size}})
        model_builder = RecommenderBuilder(
            user_model, logistic_model,
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
                batch['sequence_length_val'].append([random.randint(2, max_seq_len) * page_size])
                batch['item_idx'].append([])
                for j in range(max_seq_len):
                    for k in range(page_size):
                        if j < batch['sequence_length_val'][l][0] / page_size:
                            batch['item_idx'][l].append(random.randint(1, item_vocab_size - 1))
                        else:
                            batch['item_idx'][l].append(0)
            batches.append(batch)
        train_data = JsonListDataSet(batches)
        run_name = 'test_sequence_logistic_model_run0'
        export_dir = self._export_dir + run_name
        shutil.rmtree(export_dir, ignore_errors=True)
        model_trainer = ModelTrainer(
            train_data, builder=model_builder, max_steps=10,
            tensorboard_dir=self._test_path,
            export_dir=export_dir)
        model_trainer.train(run_name)

    def test_train_regression_model(self):
        user_vocab_size = 15
        item_vocab_size = 20
        embedding_dim = 5
        regression_dim = embedding_dim
        page_size = 3
        user_model = SVDPPUserModel(item_attrs=[])
        regression_model = RegressionPredictionModel('item', config={
            'item': {'vocab_size': item_vocab_size, 'regression_dim': regression_dim}})
        model_builder = RecommenderBuilder(
            user_model, regression_model,
            page_size=page_size,
            attr2config={
                'rating': {
                    'is_numerical': True,
                    'level': 'item'
                },
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
            embedding_attrs=['user'],
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
            batch = {'user_idx': [], 'item_idx': [], 'rating_val': [], 'sequence_length_val': []}
            for l in range(batch_size):
                batch['user_idx'].append([random.randint(1, user_vocab_size - 1)])
                batch['sequence_length_val'].append([random.randint(2, max_seq_len) * page_size])
                batch['item_idx'].append([])
                batch['rating_val'].append([])
                for j in range(max_seq_len):
                    for k in range(page_size):
                        if j < batch['sequence_length_val'][l][0] / page_size:
                            idx = random.randint(1, item_vocab_size - 1)
                            act_dice = random.random()
                            if act_dice < 0.5:
                                batch['item_idx'][l].append(idx)
                                batch['rating_val'][l].append(random.randint(1, 5))
                            else:
                                batch['item_idx'][l].append(0)
                                batch['rating_val'][l].append(0.0)
                        else:
                            batch['rating_val'][l].append(0.0)
                            batch['item_idx'][l].append(0)
            batches.append(batch)
        train_data = JsonListDataSet(batches)
        run_name = 'test_regression_l2_model_run0'
        export_dir = self._export_dir + run_name
        shutil.rmtree(export_dir, ignore_errors=True)
        model_trainer = ModelTrainer(
            train_data, builder=model_builder, max_steps=10,
            tensorboard_dir=self._test_path,
            export_dir=export_dir)
        model_trainer.train(run_name)

if __name__ == '__main__':
    unittest.main()
