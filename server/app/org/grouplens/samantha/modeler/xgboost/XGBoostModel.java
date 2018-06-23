/*
 * Copyright (c) [2016-2018] [University of Minnesota]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.grouplens.samantha.modeler.xgboost;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ml.dmlc.xgboost4j.LabeledPoint;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoostError;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.common.PredictiveModel;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.modeler.featurizer.Featurizer;
import org.grouplens.samantha.modeler.model.IndexSpace;
import org.grouplens.samantha.modeler.featurizer.StandardFeaturizer;
import org.grouplens.samantha.modeler.instance.StandardLearningInstance;
import org.grouplens.samantha.modeler.tree.TreeKey;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.IOUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XGBoostModel implements PredictiveModel, Featurizer {
    private static Logger logger = LoggerFactory.getLogger(XGBoostModel.class);
    final private StandardFeaturizer featurizer;
    final private IndexSpace indexSpace;
    private Booster booster;
    private Map<String, Integer> featureScores;

    public XGBoostModel(IndexSpace indexSpace, List<FeatureExtractor> featureExtractors,
                        List<String> features, String labelName, String weightName) {
        this.featurizer = new StandardFeaturizer(indexSpace,
                featureExtractors, features, null, labelName, weightName);
        this.indexSpace = indexSpace;
    }

    public double[] predict(LearningInstance ins) {
        if (booster == null) {
            double[] preds = new double[1];
            preds[0] = 0.0;
            return preds;
        } else {
            List<LabeledPoint> list = new ArrayList<>(1);
            list.add(((XGBoostInstance) ins).getLabeledPoint());
            try {
                DMatrix data = new DMatrix(list.iterator(), null);
                float[][] rawPreds = booster.predict(data);
                double[] preds = new double[rawPreds[0].length];
                for (int i=0; i<preds.length; i++) {
                    preds[i] = rawPreds[0][i];
                }
                return preds;
            } catch (XGBoostError e) {
                throw new BadRequestException(e);
            }
        }
    }

    public List<ObjectNode> classify(List<ObjectNode> entities) {
        List<LearningInstance> instances = new ArrayList<>();
        for (JsonNode entity : entities) {
            instances.add(featurize(entity, true));
        }
        double[][] preds = predict(instances);
        List<ObjectNode> rankings = new ArrayList<>();
        for (int i=0; i<instances.size(); i++) {
            int k = preds[i].length;
            for (int j = 0; j < k; j++) {
                if (indexSpace.getKeyMapSize(ConfigKey.LABEL_INDEX_NAME.get()) > j) {
                    ObjectNode rec = Json.newObject();
                    rec.put("dataId", i);
                    String fea = (String) indexSpace.getKeyForIndex(
                            ConfigKey.LABEL_INDEX_NAME.get(), j);
                    IOUtilities.parseEntityFromStringMap(rec, FeatureExtractorUtilities.decomposeKey(fea));
                    rec.put("classProb", preds[i][j]);
                    rankings.add(rec);
                }
            }
        }
        return rankings;
    }

    public LearningInstance featurize(JsonNode entity, boolean update) {
        return new XGBoostInstance((StandardLearningInstance) featurizer.featurize(entity, true));
    }

    public void setXGBooster(Booster booster) {
        this.booster = booster;
        try {
            Map<String, Integer> feaMap = booster.getFeatureScore(null);
            featureScores = new HashMap<>();
            for (Map.Entry<String, Integer> entry : feaMap.entrySet()) {
                String name = (String)indexSpace.getKeyForIndex(TreeKey.TREE.get(),
                        Integer.parseInt(entry.getKey().substring(1)));
                featureScores.put(name, entry.getValue());
            }
            logger.info("Number of non-zero importance features: {}", featureScores.size());
            logger.info("Feature importance: {}", Json.toJson(featureScores).toString());
        } catch (XGBoostError e) {
            throw new BadRequestException(e);
        }
    }

    public Map<String, Integer> getFeatureScores() {
        return featureScores;
    }

    public void saveModel(String modelFile) {
        try {
            this.booster.saveModel(modelFile);
        } catch (XGBoostError e) {
            throw new BadRequestException(e);
        }
    }

    public void loadModel(String modelFile) {
        try {
            ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(modelFile));
            this.booster = (Booster) inputStream.readUnshared();
        } catch (IOException | ClassNotFoundException e) {
            throw new BadRequestException(e);
        }
    }

    public void publishModel() {}
}
