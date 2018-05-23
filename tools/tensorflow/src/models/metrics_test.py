
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
                with tf.variable_scope('test1'):
                    labels = tf.constant([2], dtype=tf.int64)
                    preds = tf.constant([[0.01, 0.4, 0.1]])
                    values, updates = compute_map_metrics(labels, preds, 'MAP@1,2')
                    session.run([tf.global_variables_initializer(), tf.local_variables_initializer()])
                    session.run(updates)
                    maps = session.run(values)
                    self.assertEqual(maps[0], 0.0)
                    self.assertEqual(maps[1], 0.5)
                with tf.variable_scope('test2'):
                    labels = tf.SparseTensor(
                        tf.constant([[0, 2], [0, 5], [1, 0], [1, 2], [1, 4]], dtype=tf.int64),
                        tf.constant([2, 3, 0, 7, 1], dtype=tf.int64),
                        tf.constant([2, 6], dtype=tf.int64))
                    preds = tf.constant([
                        [0.3, 0.1, 0.9, 0.10, 0.2, 0.01, 0.003, 0.14],
                        [0.01, 0.9, 0.1, 0.2, 0.38, 0.2, 0.48, 0.04]])
                    values, updates = compute_map_metrics(labels, preds, 'MAP@1,7,8')
                    session.run([tf.global_variables_initializer(), tf.local_variables_initializer()])
                    session.run(updates)
                    maps = session.run(values)
                    self.assertAlmostEqual(maps[0], 1.000, delta=0.001)
                    self.assertAlmostEqual(maps[1], 0.547, delta=0.001)
                    self.assertAlmostEqual(maps[2], 0.610, delta=0.001)

