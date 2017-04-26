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

package org.grouplens.samantha.server.predictor;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.solver.OnlineOptimizationMethod;
import org.grouplens.samantha.modeler.solver.OptimizationMethod;
import org.grouplens.samantha.modeler.space.SpaceMode;
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
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TensorFlowPredictorConfig implements PredictorConfig {
    private final List<String> groupKeys;
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
    private final Map<String, List<String>> name2doublefeas;
    private final Map<String, List<String>> name2intfeas;
    private final String outputOperationName;
    private final String updateOperationName;
    private final String lossOperationName;
    private final String initOperationName;
    private final String graphDefFilePath;
    private final Configuration config;

    private TensorFlowPredictorConfig(List<String> groupKeys, List<String> evaluatorNames,
                                      String modelFile, String modelName,
                                      List<FeatureExtractorConfig> feaExtConfigs,
                                      Configuration entityDaoConfigs,
                                      Configuration methodConfig, Configuration onlineMethodConfig,
                                      Injector injector, Map<String, List<String>> name2doublefeas,
                                      Map<String, List<String>> name2intfeas,
                                      List<Configuration> expandersConfig, String daoConfigKey,
                                      String outputOperationName, String updateOperationName,
                                      String lossOperationName, String initOperationName,
                                      String graphDefFilePath, Configuration config) {
        this.groupKeys = groupKeys;
        this.evaluatorNames = evaluatorNames;
        this.modelFile = modelFile;
        this.modelName = modelName;
        this.feaExtConfigs = feaExtConfigs;
        this.methodConfig = methodConfig;
        this.onlineMethodConfig = onlineMethodConfig;
        this.entityDaoConfigs = entityDaoConfigs;
        this.name2doublefeas = name2doublefeas;
        this.name2intfeas = name2intfeas;
        this.outputOperationName = outputOperationName;
        this.updateOperationName = updateOperationName;
        this.lossOperationName = lossOperationName;
        this.initOperationName = initOperationName;
        this.graphDefFilePath = graphDefFilePath;
        this.injector = injector;
        this.expandersConfig = expandersConfig;
        this.daoConfigKey = daoConfigKey;
        this.config = config;
    }

    static private Map<String, List<String>> getName2Features(Configuration predictorConfig, String type) {
        Map<String, List<String>> name2feas = new HashMap<>();
        if (predictorConfig.asMap().containsKey(type)) {
            Configuration config = predictorConfig.getConfig(type);
            for (String name : config.keys()) {
                name2feas.put(name, config.getStringList(name));
            }
        }
        return name2feas;
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
        return new TensorFlowPredictorConfig(predictorConfig.getStringList("groupKeys"), evaluatorNames,
                predictorConfig.getString("modelFile"),
                predictorConfig.getString("modelName"),
                feaExtConfigs, daoConfigs,
                predictorConfig.getConfig("methodConfig"),
                predictorConfig.getConfig("onlineMethodConfig"),
                injector,
                getName2Features(predictorConfig, "name2doublefeas"),
                getName2Features(predictorConfig, "name2intfeas"), expanders,
                predictorConfig.getString("daoConfigKey"),
                predictorConfig.getString("outputOperationName"),
                predictorConfig.getString("updateOperationName"),
                predictorConfig.getString("lossOperationName"),
                predictorConfig.getString("initOperationName"),
                predictorConfig.getString("graphDefFilePath"),
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
            return producer.createTensorFlowModelModelFromGraphDef(modelName, spaceMode, graphDefFilePath,
                    groupKeys, featureExtractors, lossOperationName, updateOperationName, outputOperationName,
                    initOperationName, name2doublefeas, name2intfeas);
        }

        public Object buildModel(Object model, RequestContext requestContext) {
            JsonNode reqBody = requestContext.getRequestBody();
            TensorFlowModel tensorFlow = (TensorFlowModel) model;
            LearningData data = PredictorUtilities.getLearningData(tensorFlow, requestContext,
                    reqBody.get("learningDaoConfig"), entityDaoConfigs, expandersConfig, injector, true,
                    "", "", "", "", groupKeys);
            LearningData valid = null;
            if (reqBody.has("validationDaoConfig"))  {
                valid = PredictorUtilities.getLearningData(tensorFlow, requestContext,
                        reqBody.get("validationDaoConfig"), entityDaoConfigs, expandersConfig,
                        injector, false, "", "", "", "", groupKeys);
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
                    expandersConfig, injector, true, "", "", "", "", groupKeys);
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
