package org.grouplens.samantha.server.predictor;

import com.fasterxml.jackson.databind.JsonNode;

import org.grouplens.samantha.modeler.solver.*;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureProducer;
import org.grouplens.samantha.server.common.AbstractModelManager;
import org.grouplens.samantha.server.common.ModelManager;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.*;
import org.grouplens.samantha.server.exception.ConfigurationException;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.featurizer.FeatureExtractorConfig;
import org.grouplens.samantha.server.featurizer.FeatureExtractorListConfigParser;
import org.grouplens.samantha.server.featurizer.FeaturizerConfigParser;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.common.LearningData;
import play.Configuration;
import play.inject.Injector;

import java.util.*;

public class SVDFeaturePredictorConfig implements PredictorConfig {
    private final List<String> biasFeas;
    private final List<String> ufactFeas;
    private final List<String> ifactFeas;
    private final String modelFile;
    private final String modelName;
    private final String labelName;
    private final String weightName;
    private final int factDim;
    private final ObjectiveFunction loss;
    private final Configuration onlineMethodConfig;
    private final Configuration methodConfig;
    private final List<FeatureExtractorConfig> feaExtConfigs;
    private final Configuration entityDaoConfigs;
    private final String dependPredictorName;
    private final String dependPredictorModelName;
    private final Injector injector;
    private final List<Configuration> expandersConfig;
    private final String daoConfigKey;
    private final String serializedKey;
    private final String insName;
    private final Configuration config;

    private SVDFeaturePredictorConfig(List<String> biasFeas,
                                      List<String> ufactFeas,
                                      List<String> ifactFeas,
                                      String modelFile,
                                      String modelName,
                                      String labelName,
                                      String weightName,
                                      int factDim,
                                      ObjectiveFunction loss,
                                      Configuration onlineMethodConfig,
                                      Configuration methodConfig,
                                      List<FeatureExtractorConfig> feaExtConfigs,
                                      Configuration entityDaoConfigs,
                                      String dependPredictorName,
                                      String dependPredictorModelName,
                                      Injector injector,
                                      List<Configuration> expandersConfig,
                                      String daoConfigKey, String insName, String serializedKey,
                                      Configuration config) {
        this.biasFeas = biasFeas;
        this.ufactFeas = ufactFeas;
        this.ifactFeas = ifactFeas;
        this.modelFile = modelFile;
        this.modelName = modelName;
        this.labelName = labelName;
        this.weightName = weightName;
        this.factDim = factDim;
        this.loss = loss;
        this.methodConfig = methodConfig;
        this.onlineMethodConfig = onlineMethodConfig;
        this.feaExtConfigs = feaExtConfigs;
        this.entityDaoConfigs = entityDaoConfigs;
        this.dependPredictorModelName = dependPredictorModelName;
        this.dependPredictorName = dependPredictorName;
        this.injector = injector;
        this.expandersConfig = expandersConfig;
        this.daoConfigKey = daoConfigKey;
        this.serializedKey = serializedKey;
        this.insName = insName;
        this.config = config;
    }

    static public PredictorConfig getPredictorConfig(Configuration predictorConfig,
                                                     Injector injector)
            throws ConfigurationException {
        FeaturizerConfigParser parser = injector.instanceOf(
                FeatureExtractorListConfigParser.class);
        Configuration daoConfigs = predictorConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get());
        String dependPredictorName = null, dependPredictorModelName = null;
        if (predictorConfig.asMap().containsKey("dependPredictorName")) {
            dependPredictorName = predictorConfig.getString("dependPredictorName");
            dependPredictorModelName = predictorConfig.getString("dependPredictorModelName");
        }
        List<FeatureExtractorConfig> feaExtConfigs = parser.parse(predictorConfig
                .getConfig(ConfigKey.PREDICTOR_FEATURIZER_CONFIG.get()));
        List<Configuration> expanders = ExpanderUtilities.getEntityExpandersConfig(predictorConfig);
        try {
            return new SVDFeaturePredictorConfig(
                    predictorConfig.getStringList("biasFeas"),
                    predictorConfig.getStringList("ufactFeas"),
                    predictorConfig.getStringList("ifactFeas"),
                    predictorConfig.getString("modelFile"),
                    predictorConfig.getString("modelName"),
                    predictorConfig.getString("labelName"),
                    predictorConfig.getString("weightName"),
                    predictorConfig.getInt("factDim"),
                    (ObjectiveFunction)Class.forName(predictorConfig
                            .getString("lossFunctionClass")).newInstance(),
                    predictorConfig.getConfig("onlineOptimizationMethod"),
                    predictorConfig.getConfig("optimizationMethod"),
                    feaExtConfigs, daoConfigs,
                    dependPredictorName,
                    dependPredictorModelName, injector, expanders,
                    predictorConfig.getString("daoConfigKey"),
                    predictorConfig.getString("instanceName"),
                    predictorConfig.getString("serializedKey"), predictorConfig);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            throw new ConfigurationException(e);
        }
    }

    private class SVDFeatureModelManager extends AbstractModelManager {

        public SVDFeatureModelManager(String modelName, String modelFile, Injector injector) {
            super(injector, modelName, modelFile);
        }

        public Object createModel(RequestContext requestContext) {
            List<FeatureExtractor> featureExtractors = new ArrayList<>();
            for (FeatureExtractorConfig feaExtConfig : feaExtConfigs) {
                featureExtractors.add(feaExtConfig.getFeatureExtractor(requestContext));
            }
            SVDFeature ownModel;
            if (dependPredictorName != null) {
                SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
                configService.getPredictor(dependPredictorName, requestContext);
                String engineName = requestContext.getEngineName();
                ModelService modelService = injector.instanceOf(ModelService.class);
                SVDFeature dependModel = (SVDFeature) modelService
                        .getModel(engineName, dependPredictorModelName);
                ownModel = SVDFeature.createSVDFeatureModelFromOtherModel(dependModel, biasFeas, ufactFeas,
                        ifactFeas, labelName, weightName, featureExtractors, loss);
            } else {
                SVDFeatureProducer producer = injector.instanceOf(SVDFeatureProducer.class);
                ownModel = producer.createSVDFeatureModel(modelName,
                        biasFeas, ufactFeas, ifactFeas, labelName, weightName,
                        featureExtractors, factDim, loss);
            }
            return ownModel;
        }

        public Object buildModel(Object model, RequestContext requestContext) {
            JsonNode reqBody = requestContext.getRequestBody();
            SVDFeature svdFeature = (SVDFeature) model;
            LearningData data = PredictorUtilities.getLearningData(svdFeature, requestContext,
                    reqBody.get("learningDaoConfig"), entityDaoConfigs, expandersConfig, injector, true,
                    serializedKey, insName, labelName, weightName);
            LearningData valid = null;
            if (reqBody.has("validationDaoConfig"))  {
                valid = PredictorUtilities.getLearningData(svdFeature, requestContext,
                        reqBody.get("validationDaoConfig"), entityDaoConfigs, expandersConfig,
                        injector, false, serializedKey, insName, labelName, weightName);
            }
            OptimizationMethod method = (OptimizationMethod) PredictorUtilities
                    .getLearningMethod(methodConfig, injector, requestContext);
            method.minimize(svdFeature, data, valid);
            return model;
        }

        public Object updateModel(Object model, RequestContext requestContext) {
            SVDFeature svdFeature = (SVDFeature) model;
            LearningData data = PredictorUtilities.getLearningData(svdFeature, requestContext,
                    requestContext.getRequestBody().get(daoConfigKey), entityDaoConfigs,
                    expandersConfig, injector, true, serializedKey, insName, labelName, weightName);
            OnlineOptimizationMethod onlineMethod = (OnlineOptimizationMethod) PredictorUtilities
                    .getLearningMethod(onlineMethodConfig, injector, requestContext);
            onlineMethod.update(svdFeature, data);
            return model;
        }
    }

    public Predictor getPredictor(RequestContext requestContext) {
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        ModelManager modelManager = new SVDFeatureModelManager(modelName, modelFile, injector);
        SVDFeature model = (SVDFeature) modelManager.manage(requestContext);
        return new PredictiveModelBasedPredictor(config, model, model,
                entityDaoConfigs, injector, entityExpanders, daoConfigKey);
    }
}
