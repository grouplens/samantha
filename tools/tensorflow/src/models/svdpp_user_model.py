
import tensorflow as tf

from src.models.user_model import UserModel


class SVDPPUserModel(UserModel):

    def __init__(self, user_attr='user', item_attrs=None, relu_size=0):
        self._relu_size = relu_size
        self._user_attr = user_attr
        if item_attrs is None:
            self._item_attrs = ['item']
        else:
            self._item_attrs = item_attrs

    def _step_wise_relu(self, inputs, relu_size):
        relu_layer = tf.keras.layers.Dense(relu_size, activation='relu', dtype=tf.float32)
        return relu_layer(inputs)

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

    def _get_normalized_sum(self, embedding):
        cum_sum = tf.cumsum(embedding, axis=1)
        num = tf.expand_dims(tf.range(1, tf.shape(embedding)[1] + 1), 1)
        tiled_num = tf.tile(num, [1, tf.shape(embedding)[2]])
        return cum_sum / tf.cast(tiled_num, tf.float32)

    def get_user_model(self, max_seq_len, sequence_length, attr2embedding, attr2config):
        if self._relu_size > 0:
            concatenated = self._get_concat_embeddings(max_seq_len, attr2embedding, attr2config)
            relu_output = self._step_wise_relu(concatenated, self._relu_size)
            user_model = self._get_normalized_sum(relu_output)
        else:
            user_model = tf.tile(
                    attr2embedding[self._user_attr], [1, max_seq_len, 1])
            for attr in self._item_attrs:
                user_model += self._get_normalized_sum(attr2embedding[attr])
        return user_model
