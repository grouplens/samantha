
import tensorflow as tf

from src.models.user_model import UserModel


class SequenceUserModel(UserModel):

    def __init__(self, rnn_size):
        self._rnn_size = rnn_size

    def _step_wise_relu(self, inputs, relu_size):
        relu_layer = tf.keras.layers.Dense(relu_size, activation='relu', dtype=tf.float32)
        return relu_layer(inputs)

    def _get_rnn_output(self, inputs, rnn_size):
        rnn_layer = tf.keras.layers.GRU(rnn_size, return_sequences=True, dtype=tf.float32)
        return rnn_layer(inputs)

    def _get_concat_embeddings(self, max_seq_len, attr2embedding, attr2config):
        embeddings = []
        for attr, embedding in attr2embedding.iteritems():
            config = attr2config[attr]
            if config['level'] == 'user':
                embedding = tf.tile(
                    embedding, [1, max_seq_len, 1])
            embeddings.append(embedding)
        concatenated = tf.concat(embeddings, 2)
        return concatenated

    def get_user_model(self, max_seq_len, sequence_length, attr2embedding, attr2config):
        concatenated = self._get_concat_embeddings(max_seq_len, attr2embedding, attr2config)
        relu_output = self._step_wise_relu(concatenated, self._rnn_size)
        rnn_output = self._get_rnn_output(relu_output, self._rnn_size)
        return rnn_output

