

class PredictionModel:

    def __init__(self):
        pass

    def get_target_paras(self, target, config):
        raise Exception('This must be overridden.')

    def get_target_loss(self, used_model, labels, indices, user_model,
            paras, target, config, mode, context):
        raise Exception('This must be overridden.')

    def get_target_prediction(self, used_model, paras, target, config):
        raise Exception('This must be overridden.')

    def get_item_prediction(self, used_model, paras, items, target, config):
        raise Exception('This must be overridden.')

