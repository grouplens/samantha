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

package org.grouplens.samantha.server.xgboost;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.model.SpaceMode;
import org.grouplens.samantha.modeler.xgboost.XGBoostMethod;
import org.grouplens.samantha.modeler.xgboost.XGBoostModel;
import org.grouplens.samantha.modeler.xgboost.XGBoostModelProducer;
import org.grouplens.samantha.server.common.AbstractModelManager;
import org.grouplens.samantha.server.common.ModelManager;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.featurizer.FeatureExtractorConfig;
import org.grouplens.samantha.server.featurizer.FeatureExtractorListConfigParser;
import org.grouplens.samantha.server.featurizer.FeaturizerConfigParser;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.PredictiveModelBasedPredictor;
import org.grouplens.samantha.server.predictor.Predictor;
import org.grouplens.samantha.server.predictor.PredictorConfig;
import org.grouplens.samantha.server.predictor.PredictorUtilities;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class XGBoostPredictorConfig implements PredictorConfig {
    private final String modelName;
    private final String modelFile;
    private final List<FeatureExtractorConfig> feaExtConfigs;
    private final List<String> features;
    private final String labelName;
    private final String weightName;
    private final Configuration daoConfigs;
    private final List<Configuration> expandersConfig;
    private final Injector injector;
    private final XGBoostMethod method;
    private final String daoConfigKey;
    private final Configuration config;

    private XGBoostPredictorConfig(String modelName, List<FeatureExtractorConfig> feaExtConfigs,
                                   List<String> features, String labelName, String weightName,
                                   Configuration daoConfigs, List<Configuration> expandersConfig,
                                   Injector injector, XGBoostMethod method, String modelFile,
                                   String daoConfigKey, Configuration config) {
        this.modelName = modelName;
        this.feaExtConfigs = feaExtConfigs;
        this.features = features;
        this.labelName = labelName;
        this.weightName = weightName;
        this.daoConfigs = daoConfigs;
        this.expandersConfig = expandersConfig;
        this.injector = injector;
        this.method = method;
        this.modelFile = modelFile;
        this.daoConfigKey = daoConfigKey;
        this.config = config;
    }

    public static PredictorConfig getPredictorConfig(Configuration predictorConfig,
                                                     Injector injector) {
        FeaturizerConfigParser parser = injector.instanceOf(
                FeatureExtractorListConfigParser.class);
        Configuration daoConfigs = predictorConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get());
        List<FeatureExtractorConfig> feaExtConfigs = parser.parse(predictorConfig
                .getConfig(ConfigKey.PREDICTOR_FEATURIZER_CONFIG.get()));
        List<Configuration> expanders = ExpanderUtilities.getEntityExpandersConfig(predictorConfig);
        int round = predictorConfig.getInt("numTrees");
        return new XGBoostPredictorConfig(predictorConfig.getString("modelName"),
                feaExtConfigs, predictorConfig.getStringList("features"),
                predictorConfig.getString("labelName"),
                predictorConfig.getString("weightName"), daoConfigs, expanders, injector,
                new XGBoostMethod(predictorConfig.getConfig("methodConfig").asMap(), round),
                predictorConfig.getString("modelFile"),
                predictorConfig.getString("daoConfigKey"), predictorConfig);
    }

    private class XGBoostModelManager extends AbstractModelManager {

        public XGBoostModelManager(String modelName, String modelFile, Injector injector) {
            super(injector, modelName, modelFile, null);
        }

        public Object createModel(RequestContext requestContext, SpaceMode spaceMode) {
            List<FeatureExtractor> featureExtractors = new ArrayList<>();
            for (FeatureExtractorConfig feaExtConfig : feaExtConfigs) {
                featureExtractors.add(feaExtConfig.getFeatureExtractor(requestContext));
            }
            XGBoostModelProducer producer = injector.instanceOf(XGBoostModelProducer.class);
            XGBoostModel model = producer.createXGBoostModel(modelName, spaceMode,
                    featureExtractors, features,
                    labelName, weightName);
            return model;
        }

        public Object buildModel(Object model, RequestContext requestContext) {
            JsonNode reqBody = requestContext.getRequestBody();
            XGBoostModel xgBoost = (XGBoostModel) model;
            LearningData learnData = PredictorUtilities.getLearningData(xgBoost, requestContext,
                    reqBody.get("learningDaoConfig"), daoConfigs,
                    expandersConfig, injector, true, null, 128);
            LearningData validData = null;
            if (reqBody.has("validationDaoConfig")) {
                validData = PredictorUtilities.getLearningData(xgBoost, requestContext,
                        reqBody.get("validationDaoConfig"), daoConfigs,
                        expandersConfig, injector, true, null, 128);
            }
            method.learn(xgBoost, learnData, validData);
            return model;
        }
    }

    public Predictor getPredictor(RequestContext requestContext) {
        ModelManager modelManager = new XGBoostModelManager(modelName, modelFile, injector);
        XGBoostModel model = (XGBoostModel) modelManager.manage(requestContext);
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        return new PredictiveModelBasedPredictor(config, model, model,
                daoConfigs, injector, entityExpanders, daoConfigKey);
    }
}
