
import unittest
import random
import numpy as np
import tensorflow as tf

from src.models.metrics import *


class MetricsTest(unittest.TestCase):

    def test_compute_map_metrics(self):
        graph = tf.Graph()
        with graph.as_default():
            session = tf.Session(graph=graph)
            with session.as_default():
                labels = tf.random_uniform([10], dtype=tf.int64, maxval=99)
                preds = tf.random_uniform([10, 100], dtype=tf.float32)
                values, updates = compute_map_metrics(labels, preds, 'MAP@1,5,10')
                session.run([tf.global_variables_initializer(), tf.local_variables_initializer()])
                session.run([updates, values])

    def test_compute_ap_metrics(self):
        pass

    def test_compute_ar_metrics(self):
        pass

    def test_compute_auc_metric(self):
        pass

    def test_get_eval_user_model(self):
        pass

    def test_compute_eval_label_metrics(self):
        pass

    def test_get_per_step_eval_user_model(self):
        pass

    def test_compute_per_step_eval_label_metrics(self):
        pass
