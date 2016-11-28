package org.grouplens.samantha.server.predictor;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.solver.OnlineOptimizationMethod;
import org.grouplens.samantha.modeler.reinforce.LinearUCB;
import org.grouplens.samantha.modeler.reinforce.LinearUCBProducer;
import org.grouplens.samantha.server.common.AbstractModelManager;
import org.grouplens.samantha.server.common.ModelManager;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.featurizer.FeatureExtractorConfig;
import org.grouplens.samantha.server.featurizer.FeatureExtractorListConfigParser;
import org.grouplens.samantha.server.featurizer.FeaturizerConfigParser;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class LinearUCBPredictorConfig implements PredictorConfig {
    private final String modelName;
    private final String modelFile;
    private final List<FeatureExtractorConfig> feaExtConfigs;
    private final List<String> features;
    private final String labelName;
    private final String weightName;
    private final Configuration daoConfigs;
    private final List<Configuration> expandersConfig;
    private final Injector injector;
    private final Configuration methodConfig;
    private final String daoConfigKey;
    private final double lambda;
    private final double alpha;
    private final int numMainFeatures;
    private final String serializedKey;
    private final String insName;
    private final Configuration config;

    private LinearUCBPredictorConfig(String modelName, List<FeatureExtractorConfig> feaExtConfigs,
                                     List<String> features, int numMainFeatures, String labelName, String weightName,
                                     Configuration daoConfigs, List<Configuration> expandersConfig,
                                     Injector injector, Configuration methodConfig, String modelFile,
                                     String daoConfigKey, double lambda, double alpha,
                                     String insName, String serializedKey, Configuration config) {
        this.modelName = modelName;
        this.feaExtConfigs = feaExtConfigs;
        this.features = features;
        this.labelName = labelName;
        this.weightName = weightName;
        this.daoConfigs = daoConfigs;
        this.expandersConfig = expandersConfig;
        this.injector = injector;
        this.methodConfig = methodConfig;
        this.modelFile = modelFile;
        this.daoConfigKey = daoConfigKey;
        this.lambda = lambda;
        this.alpha = alpha;
        this.numMainFeatures = numMainFeatures;
        this.serializedKey = serializedKey;
        this.insName = insName;
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
        double alpha = 0.1;
        if (predictorConfig.asMap().containsKey("alpha")) {
            alpha = predictorConfig.getDouble("alpha");
        }
        double lambda = 1.0;
        if (predictorConfig.asMap().containsKey("lambda")) {
            lambda = predictorConfig.getDouble("lambda");
        }
        return new LinearUCBPredictorConfig(predictorConfig.getString("modelName"),
                feaExtConfigs, predictorConfig.getStringList("features"),
                predictorConfig.getInt("numMainFeatures"),
                predictorConfig.getString("labelName"),
                predictorConfig.getString("weightName"), daoConfigs, expanders, injector,
                predictorConfig.getConfig("onlineOptimizationMethod"),
                predictorConfig.getString("modelFile"),
                predictorConfig.getString("daoConfigKey"), lambda, alpha,
                predictorConfig.getString("instanceName"),
                predictorConfig.getString("serializedKey"), predictorConfig);
    }

    private class LinearUCBModelManager extends AbstractModelManager {

        public LinearUCBModelManager(String modelName, String modelFile, Injector injector) {
            super(injector, modelName, modelFile);
        }

        public Object createModel(RequestContext requestContext) {
            List<FeatureExtractor> featureExtractors = new ArrayList<>();
            for (FeatureExtractorConfig feaExtConfig : feaExtConfigs) {
                featureExtractors.add(feaExtConfig.getFeatureExtractor(requestContext));
            }
            LinearUCBProducer producer = injector.instanceOf(LinearUCBProducer.class);
            LinearUCB model = producer.createLinearUCBModel(modelName, features, numMainFeatures,
                    labelName, weightName, alpha, lambda, featureExtractors);
            return model;
        }

        public Object buildModel(Object model, RequestContext requestContext) {
            LinearUCB ucbModel = (LinearUCB) model;
            JsonNode reqBody = requestContext.getRequestBody();
            LearningData data = PredictorUtilities.getLearningData(ucbModel, requestContext,
                    reqBody.get("learningDaoConfig"), daoConfigs, expandersConfig, injector, true,
                    serializedKey, insName, labelName, weightName);
            LearningData valid = null;
            if (reqBody.has("validationDaoConfig"))  {
                valid = PredictorUtilities.getLearningData(ucbModel, requestContext,
                        reqBody.get("validationDaoConfig"), daoConfigs, expandersConfig,
                        injector, false, serializedKey, insName, labelName, weightName);
            }
            OnlineOptimizationMethod method = (OnlineOptimizationMethod) PredictorUtilities
                    .getLearningMethod(methodConfig, injector, requestContext);
            method.minimize(ucbModel, data, valid);
            return model;
        }

        public Object updateModel(Object model, RequestContext requestContext) {
            LinearUCB ucbModel = (LinearUCB) model;
            LearningData data = PredictorUtilities.getLearningData(ucbModel, requestContext,
                    requestContext.getRequestBody().get(daoConfigKey), daoConfigs,
                    expandersConfig, injector, true, serializedKey, insName, labelName, weightName);
            OnlineOptimizationMethod onlineMethod = (OnlineOptimizationMethod) PredictorUtilities
                    .getLearningMethod(methodConfig, injector, requestContext);
            onlineMethod.update(ucbModel, data);
            return ucbModel;
        }
    }

    public Predictor getPredictor(RequestContext requestContext) {
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        ModelManager modelManager = new LinearUCBModelManager(modelName, modelFile, injector);
        LinearUCB model = (LinearUCB) modelManager.manage(requestContext);
        return new PredictiveModelBasedPredictor(config, model, model,
                daoConfigs, injector, entityExpanders, daoConfigKey);
    }
}
