/*
 * Copyright (c) [2016-2017] [University of Minnesota]
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

package org.grouplens.samantha.server.tensorflow;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.solver.OnlineOptimizationMethod;
import org.grouplens.samantha.modeler.solver.OptimizationMethod;
import org.grouplens.samantha.modeler.model.SpaceMode;
import org.grouplens.samantha.modeler.tensorflow.TensorFlowModel;
import org.grouplens.samantha.modeler.tensorflow.TensorFlowModelProducer;
import org.grouplens.samantha.server.common.AbstractModelManager;
import org.grouplens.samantha.server.common.ModelManager;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.exception.ConfigurationException;
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

public class TensorFlowPredictorConfig implements PredictorConfig {
    private final List<String> groupKeys;
    private final List<String> feedFeas;
    private final List<List<String>> equalSizeChecks;
    private final List<String> indexKeys;
    private final List<String> evaluatorNames;
    private final String modelFile;
    private final String modelName;
    private final List<FeatureExtractorConfig> feaExtConfigs;
    private final Configuration entityDaoConfigs;
    private final Configuration onlineMethodConfig;
    private final Configuration methodConfig;
    private final Injector injector;
    private final List<Configuration> expandersConfig;
    private final String daoConfigKey;
    private final String outputOper;
    private final String updateOper;
    private final String lossOper;
    private final String initOper;
    private final String topKOper;
    private final String topKId;
    private final String topKValue;
    private final String itemIndex;
    private final String graphDefFilePath;
    private final String modelExportDir;
    private final Configuration config;

    private TensorFlowPredictorConfig(List<String> groupKeys, List<String> feedFeas, List<String> indexKeys,
                                      List<List<String>> equalSizeChecks, List<String> evaluatorNames,
                                      String modelFile, String modelName,
                                      List<FeatureExtractorConfig> feaExtConfigs,
                                      Configuration entityDaoConfigs,
                                      Configuration methodConfig, Configuration onlineMethodConfig,
                                      Injector injector,
                                      List<Configuration> expandersConfig, String daoConfigKey,
                                      String outputOper, String updateOper,
                                      String lossOper, String initOper, String topKOper,
                                      String topKId, String topKValue, String itemIndex,
                                      String graphDefFilePath, String modelExportDir, Configuration config) {
        this.groupKeys = groupKeys;
        this.feedFeas = feedFeas;
        this.indexKeys = indexKeys;
        this.equalSizeChecks = equalSizeChecks;
        this.evaluatorNames = evaluatorNames;
        this.modelFile = modelFile;
        this.modelName = modelName;
        this.feaExtConfigs = feaExtConfigs;
        this.methodConfig = methodConfig;
        this.onlineMethodConfig = onlineMethodConfig;
        this.entityDaoConfigs = entityDaoConfigs;
        this.outputOper = outputOper;
        this.updateOper = updateOper;
        this.lossOper = lossOper;
        this.initOper = initOper;
        this.topKId = topKId;
        this.topKValue = topKValue;
        this.topKOper = topKOper;
        this.itemIndex = itemIndex;
        this.graphDefFilePath = graphDefFilePath;
        this.modelExportDir = modelExportDir;
        this.injector = injector;
        this.expandersConfig = expandersConfig;
        this.daoConfigKey = daoConfigKey;
        this.config = config;
    }

    static public PredictorConfig getPredictorConfig(Configuration predictorConfig,
                                                     Injector injector)
            throws ConfigurationException {
        FeaturizerConfigParser parser = injector.instanceOf(
                FeatureExtractorListConfigParser.class);
        Configuration daoConfigs = predictorConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get());
        List<FeatureExtractorConfig> feaExtConfigs = parser.parse(predictorConfig
                .getConfig(ConfigKey.PREDICTOR_FEATURIZER_CONFIG.get()));
        List<Configuration> expanders = ExpanderUtilities.getEntityExpandersConfig(predictorConfig);
        List<String> evaluatorNames = new ArrayList<>();
        if (predictorConfig.asMap().containsKey("evaluatorNames")) {
            evaluatorNames = predictorConfig.getStringList("evaluatorNames");
        }
        List<Configuration> checkConfigs = predictorConfig.getConfigList("equalSizeChecks");
        List<List<String>> equalSizeChecks = new ArrayList<>();
        if (checkConfigs != null) {
            for (Configuration check : checkConfigs) {
                equalSizeChecks.add(check.getStringList("featuresWithEqualSizes"));
            }
        }
        return new TensorFlowPredictorConfig(
                predictorConfig.getStringList("groupKeys"),
                predictorConfig.getStringList("feedFeas"),
                predictorConfig.getStringList("indexKeys"),
                equalSizeChecks, evaluatorNames,
                predictorConfig.getString("modelFile"),
                predictorConfig.getString("modelName"),
                feaExtConfigs, daoConfigs,
                predictorConfig.getConfig("methodConfig"),
                predictorConfig.getConfig("onlineMethodConfig"),
                injector, expanders,
                predictorConfig.getString("daoConfigKey"),
                predictorConfig.getString("outputOper"),
                predictorConfig.getString("updateOper"),
                predictorConfig.getString("lossOper"),
                predictorConfig.getString("initOper"),
                predictorConfig.getString("topKOper"),
                predictorConfig.getString("topKId"),
                predictorConfig.getString("topKValue"),
                predictorConfig.getString("itemIndex"),
                predictorConfig.getString("graphDefFilePath"),
                predictorConfig.getString("modelExportDir"),
                predictorConfig);
    }

    private class TensorFlowModelManager extends AbstractModelManager {

        public TensorFlowModelManager(String modelName, String modelFile, Injector injector,
                                      List<String> evaluatorNames) {
            super(injector, modelName, modelFile, evaluatorNames);
        }

        public Object createModel(RequestContext requestContext, SpaceMode spaceMode) {
            List<FeatureExtractor> featureExtractors = new ArrayList<>();
            for (FeatureExtractorConfig feaExtConfig : feaExtConfigs) {
                featureExtractors.add(feaExtConfig.getFeatureExtractor(requestContext));
            }
            TensorFlowModelProducer producer = injector.instanceOf(TensorFlowModelProducer.class);
            if (graphDefFilePath != null) {
                return producer.createTensorFlowModelModelFromGraphDef(
                        modelName, spaceMode, graphDefFilePath,
                        groupKeys, feedFeas, equalSizeChecks, indexKeys,
                        featureExtractors,
                        lossOper, updateOper,
                        outputOper, initOper, topKOper,
                        topKId, topKValue, itemIndex);
            } else if (modelExportDir != null) {
                return producer.createTensorFlowModelModelFromExportDir(
                        modelName, spaceMode, modelExportDir,
                        groupKeys, feedFeas, equalSizeChecks, indexKeys,
                        featureExtractors,
                        lossOper, updateOper,
                        outputOper, initOper, topKOper,
                        topKId, topKValue, itemIndex);
            } else {
                throw new ConfigurationException("graphDefFilePath or modelExportDir must be provided.");
            }
        }

        public Object buildModel(Object model, RequestContext requestContext) {
            JsonNode reqBody = requestContext.getRequestBody();
            TensorFlowModel tensorFlow = (TensorFlowModel) model;
            LearningData data = PredictorUtilities.getLearningData(tensorFlow, requestContext,
                    reqBody.get("learningDaoConfig"), entityDaoConfigs, expandersConfig,
                    injector, true, groupKeys, 128);
            LearningData valid = null;
            if (reqBody.has("validationDaoConfig"))  {
                valid = PredictorUtilities.getLearningData(tensorFlow, requestContext,
                        reqBody.get("validationDaoConfig"), entityDaoConfigs, expandersConfig,
                        injector, true, groupKeys, 128);
            }
            OptimizationMethod method = (OptimizationMethod) PredictorUtilities
                    .getLearningMethod(methodConfig, injector, requestContext);
            method.minimize(tensorFlow, data, valid);
            return model;
        }

        public Object updateModel(Object model, RequestContext requestContext) {
            TensorFlowModel tensorFlow = (TensorFlowModel) model;
            LearningData data = PredictorUtilities.getLearningData(tensorFlow, requestContext,
                    requestContext.getRequestBody().get(daoConfigKey), entityDaoConfigs,
                    expandersConfig, injector, true, groupKeys, 128);
            OnlineOptimizationMethod onlineMethod = (OnlineOptimizationMethod) PredictorUtilities
                    .getLearningMethod(onlineMethodConfig, injector, requestContext);
            onlineMethod.update(tensorFlow, data);
            return model;
        }
    }

    public Predictor getPredictor(RequestContext requestContext) {
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        ModelManager modelManager = new TensorFlowModelManager(modelName, modelFile, injector, evaluatorNames);
        TensorFlowModel model = (TensorFlowModel) modelManager.manage(requestContext);
        return new PredictiveModelBasedPredictor(config, model, model,
                entityDaoConfigs, injector, entityExpanders, daoConfigKey);
    }
}
