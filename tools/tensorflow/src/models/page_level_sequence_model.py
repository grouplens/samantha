
import tensorflow as tf

from ..builder import ModelBuilder

class PageLevelSequenceModelBuilder(ModelBuilder):

    def __init__(self,
                 page_size=24,
                 user_vocab_size=10000,
                 item_vocab_size=10000,
                 rnn_size=128,
                 item_events=['display', 'click', 'high_rate', 'low_rate', 'wishlist']):
        self._page_size = page_size
        self._user_vocab_size = user_vocab_size
        self._item_vocab_size = item_vocab_size
        self._rnn_size = rnn_size
        self._item_events = item_events

    def _event_item(self, name):
        event = tf.placeholder(
            tf.int32, shape=(None, None), name='%s_idx' % name)
        sequence_len = tf.shape(event)[1] / self._page_size
        recognized_item = event * (event < self._item_vocab_size)
        return tf.reshape(event, [tf.shape(recognized_item)[0], sequence_len, self._page_size])

    def build_model(self):
        #get [batch, sequence_length, concatenated_embedding]
        item_idx = {}
        for key in self._item_events:
            item_idx[key] = self._event_item(key)
        #create item input embeddings
        event_embedder = {}
        for key in self._item_events:
            event_embedder[key] = tf.keras.layers.Embedding(self._item_vocab_size, self._rnn_size)
        #get embeddings for each event
        event_embedding = []
        for key in self._item_events:
            event_embedding.append(event_embedder[key](item_idx[key]))
        concated_event_embedding = tf.concat(event_embedding, 3)
        item_input = tf.reshape(concated_event_embedding,
                            [tf.shape(concated_event_embedding)[0],
                             tf.shape(concated_event_embedding)[1], -1])
        user_idx = tf.placeholder(tf.int32, shape=(None, 1), name='user_idx')
        #create user input embedding
        user_embedder = tf.keras.layers.Embedding(self._user_vocab_size, self._rnn_size)
        expanded_user = tf.tile(user_idx, [1, tf.shape(item_input)[1]])
        expanded_user_embedding = user_embedder(expanded_user)
        all_input = tf.concate([item_input, expanded_user_embedding], 2)

        #go through relu to have [batch, sequence_length, relu_output]
        relu_output = self._step_wise_relu(all_input)

        #go through rnn and get all the states output [batch, sequence_length, rnn_state]
        rnn_output = self._get_rnn_output(relu_output)

        #create item softmax weights and biases
        softmax_weights = {}
        softmax_biases = {}
        for key in self._item_events:
            softmax_weights[key] = tf.keras.layers.Embedding(self._item_vocab_size, self._rnn_size)
            softmax_biases[key] = tf.keras.layers.Embedding(self._item_vocab_size, 1)

        #get softmax prediction and losses for all steps excluding the zeros

        #get softmax prediction by taking into account all steps in the sequences excluding the zeros

        loss = None
        return loss
