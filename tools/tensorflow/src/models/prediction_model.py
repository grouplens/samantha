

class PredictionModel:

    def __init__(self):
        pass

    def get_target_paras(self, target, config):
        raise Exception('This must be overridden.')

    def get_target_loss(self, user_model, labels, label_shape, indices, paras, target, config, mode):
        raise Exception('This must be overridden.')

    def get_target_prediction(self, user_model, paras, target, config):
        raise Exception('This must be overridden.')

