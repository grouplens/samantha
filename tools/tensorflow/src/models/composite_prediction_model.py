
from src.models.prediction_model import PredictionModel
from src.models.softmax_prediction_model import SoftmaxPredictionModel

class CompositePredictionModel(PredictionModel):

    def __init__(self, target2model=None):
        if target2model is None:
            self._target2model = {
                'item': SoftmaxPredictionModel()
            }
        else:
            self._target2model = target2model

    def get_target_paras(self, target, config):
        return self._target2model[target].get_target_paras(target, config)

    def get_target_loss(self, used_model, labels, indices, user_model,
            paras, target, config, mode, context):
        return self._target2model[target].get_target_loss(used_model, labels, indices, user_model,
                                                          paras, target, config, mode, context)

    def get_target_prediction(self, used_model, indices, paras, target, config, context):
        return self._target2model[target].get_target_prediction(used_model, indices, paras, target, config, context)

