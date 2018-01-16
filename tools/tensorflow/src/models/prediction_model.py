

class PredictionModel:

    def __init__(self):
        pass

    def get_target_paras(self, target, config):
        raise Exception('This must be overridden.')

    def get_target_prediction_loss(self, user_model, labels, paras, target, config):
        raise Exception('This must be overridden.')

    def get_target_prediction(self, user_model, pred_paras, target2preds, target, config):
        raise Exception('This must be overridden.')

