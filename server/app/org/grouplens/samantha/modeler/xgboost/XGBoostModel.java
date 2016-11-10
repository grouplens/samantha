package org.grouplens.samantha.modeler.xgboost;

import com.fasterxml.jackson.databind.JsonNode;
import ml.dmlc.xgboost4j.LabeledPoint;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoostError;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.common.PredictiveModel;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.featurizer.Featurizer;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.featurizer.StandardFeaturizer;
import org.grouplens.samantha.modeler.featurizer.StandardLearningInstance;
import org.grouplens.samantha.server.exception.InvalidRequestException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

public class XGBoostModel implements PredictiveModel, Featurizer {
    final private StandardFeaturizer featurizer;
    private Booster booster;

    public XGBoostModel(IndexSpace indexSpace, List<FeatureExtractor> featureExtractors,
                        List<String> features, String labelName, String weightName) {
        this.featurizer = new StandardFeaturizer(indexSpace, featureExtractors, features, labelName, weightName);
    }

    public double predict(LearningInstance ins) {
        if (booster == null) {
            return 0.0;
        } else {
            List<LabeledPoint> list = new ArrayList<>(1);
            list.add(((XGBoostInstance) ins).getLabeledPoint());
            try {
                DMatrix data = new DMatrix(list.iterator(), null);
                return booster.predict(data)[0][0];
            } catch (XGBoostError e) {
                throw new InvalidRequestException(e);
            }
        }
    }

    public LearningInstance featurize(JsonNode entity, boolean update) {
        return new XGBoostInstance((StandardLearningInstance) featurizer.featurize(entity, update));
    }

    public void setXGBooster(Booster booster) {
        this.booster = booster;
    }

    public void saveModel(String modelFile) {
        try {
            this.booster.saveModel(modelFile);
        } catch (XGBoostError e) {
            throw new InvalidRequestException(e);
        }
    }

    public void loadModel(String modelFile) {
        try {
            ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(modelFile));
            this.booster = (Booster) inputStream.readUnshared();
        } catch (IOException | ClassNotFoundException e) {
            throw new InvalidRequestException(e);
        }
    }
}
