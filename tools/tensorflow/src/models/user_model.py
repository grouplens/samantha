

class UserModel:

    def __init__(self):
        pass

    def get_user_model(self, max_seq_len, sequence_length, attr2embedding, attr2config):
        raise Exception('This must be overridden.')