
import tensorflow as tf


def step_wise_relu(inputs, relu_size):
    relu_layer = tf.keras.layers.Dense(relu_size, activation='relu', dtype=tf.float32)
    return relu_layer(inputs)


def get_concat_embeddings(max_seq_len, attr2embedding, attr2config):
    embeddings = []
    for attr, embedding in attr2embedding.iteritems():
        config = attr2config[attr]
        if config['level'] == 'user':
            embedding = tf.tile(
                embedding, [1, max_seq_len, 1])
        embeddings.append(embedding)
    concatenated = tf.concat(embeddings, 2)
    return concatenated


def get_normalized_sum(embedding):
    cum_sum = tf.cumsum(embedding, axis=1)
    num = tf.expand_dims(tf.range(1, tf.shape(embedding)[1] + 1), 1)
    tiled_num = tf.tile(num, [1, tf.shape(embedding)[2]])
    return cum_sum / tf.cast(tiled_num, tf.float32)


def get_rnn_output(inputs, rnn_size):
    rnn_layer = tf.keras.layers.GRU(rnn_size, return_sequences=True, dtype=tf.float32)
    return rnn_layer(inputs)
