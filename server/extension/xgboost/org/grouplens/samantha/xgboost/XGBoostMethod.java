package org.grouplens.samantha.xgboost;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningMethod;
import org.grouplens.samantha.modeler.common.PredictiveModel;
import org.grouplens.samantha.server.exception.InvalidRequestException;

import java.util.HashMap;
import java.util.Map;

public class XGBoostMethod implements LearningMethod {
    final private Map<String, Object> params;
    final private int round;

    public XGBoostMethod(Map<String, Object> params, int round) {
        this.params = params;
        this.round = round;
    }

    public void learn(PredictiveModel model, LearningData learningData, LearningData validData) {
        try {
            DMatrix dtrain = new DMatrix(new XGBoostIterator(learningData), null);
            Map<String, DMatrix> watches = new HashMap<>();
            if (validData != null) {
                watches.put("Validation", new DMatrix(new XGBoostIterator(validData), null));
            }
            Booster booster = XGBoost.train(dtrain, params, round, watches, null, null);
            XGBoostModel boostModel = (XGBoostModel) model;
            boostModel.setXGBooster(booster);
        } catch (XGBoostError e) {
            throw new InvalidRequestException(e);
        }
    }
}
