
import tensorflow as tf

from src.models import layers
from src.models.user_model import UserModel


class SequenceUserModel(UserModel):

    def __init__(self, rnn_size, use_relu=True, cell_type='gru'):
        self._rnn_size = rnn_size
        self._use_relu = use_relu
        self._cell_type = cell_type

    def get_user_model(self, max_seq_len, sequence_length, attr2embedding, attr2config):
        concatenated = layers.get_concat_embeddings(max_seq_len, attr2embedding, attr2config)
        if self._use_relu:
            rnn_input = layers.step_wise_relu(concatenated, self._rnn_size)
        else:
            rnn_input = concatenated
        if self._cell_type == 'lstm':
            rnn_output = layers.get_lstm_output(rnn_input, self._rnn_size)
        elif self._cell_type == 'gru':
            rnn_output = layers.get_gru_output(rnn_input, self._rnn_size)
        return rnn_output

