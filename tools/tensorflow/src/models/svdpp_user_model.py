
import tensorflow as tf

from src.models import layers
from src.models.user_model import UserModel


class SVDPPUserModel(UserModel):

    def __init__(self, user_attr='user', item_attrs=None, relu_size=0):
        self._relu_size = relu_size
        self._user_attr = user_attr
        if item_attrs is None:
            self._item_attrs = ['item']
        else:
            self._item_attrs = item_attrs

    def get_user_model(self, max_seq_len, sequence_length, attr2embedding, attr2config):
        if self._relu_size > 0:
            concatenated = layers.get_concat_embeddings(max_seq_len, attr2embedding, attr2config)
            relu_output = layers.step_wise_relu(concatenated, self._relu_size)
            user_model = layers.get_normalized_sum(relu_output)
        else:
            user_model = tf.tile(
                    attr2embedding[self._user_attr], [1, max_seq_len, 1])
            for attr in self._item_attrs:
                user_model += layers.get_normalized_sum(attr2embedding[attr])
        return user_model
