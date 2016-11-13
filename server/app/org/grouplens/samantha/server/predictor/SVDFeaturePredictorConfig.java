package org.grouplens.samantha.server.predictor;

import com.fasterxml.jackson.databind.JsonNode;

import org.grouplens.samantha.modeler.solver.*;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureModelProducer;
import org.grouplens.samantha.server.common.ModelOperation;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.*;
import org.grouplens.samantha.server.exception.ConfigurationException;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.featurizer.FeatureExtractorConfig;
import org.grouplens.samantha.server.featurizer.FeatureExtractorListConfigParser;
import org.grouplens.samantha.server.featurizer.FeaturizerConfigParser;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureModel;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.server.retriever.RetrieverUtilities;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

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

    private void updateModel(SVDFeatureModel model, RequestContext requestContext) {
        LearningData data = PredictorUtilities.getLearningData(model, requestContext,
                requestContext.getRequestBody().get(daoConfigKey), entityDaoConfigs,
                expandersConfig, injector, true, serializedKey, insName, labelName, weightName);
        OnlineOptimizationMethod onlineMethod = (OnlineOptimizationMethod) PredictorUtilities
                .getLearningMethod(onlineMethodConfig, injector, requestContext);
        onlineMethod.update(model, data);
    }

    private SVDFeatureModel createNewModel(ModelService modelService, String engineName,
                                           RequestContext requestContext) {
        List<FeatureExtractor> featureExtractors = new ArrayList<>();
        for (FeatureExtractorConfig feaExtConfig : feaExtConfigs) {
            featureExtractors.add(feaExtConfig.getFeatureExtractor(requestContext));
        }
        SVDFeatureModel ownModel;
        if (dependPredictorName != null) {
            SVDFeatureModel dependModel = (SVDFeatureModel) modelService
                    .getModel(engineName, dependPredictorModelName);
            ownModel = SVDFeatureModel.createSVDFeatureModelFromOtherModel(dependModel, biasFeas, ufactFeas,
                    ifactFeas, labelName, weightName, featureExtractors, loss);
        } else {
            SVDFeatureModelProducer producer = injector.instanceOf(SVDFeatureModelProducer.class);
            ownModel = producer.createSVDFeatureModel(modelName,
                    biasFeas, ufactFeas, ifactFeas, labelName, weightName,
                    featureExtractors, factDim, loss);
        }
        return ownModel;
    }

    private SVDFeatureModel buildModel(RequestContext requestContext, ModelService modelService,
                                       String engineName) {
        SVDFeatureModel model = createNewModel(modelService, engineName, requestContext);
        JsonNode reqBody = requestContext.getRequestBody();
        LearningData data = PredictorUtilities.getLearningData(model, requestContext,
                reqBody.get("learningDaoConfig"), entityDaoConfigs, expandersConfig, injector, true,
                serializedKey, insName, labelName, weightName);
        LearningData valid = null;
        if (reqBody.has("validationDaoConfig"))  {
            valid = PredictorUtilities.getLearningData(model, requestContext,
                    reqBody.get("validationDaoConfig"), entityDaoConfigs, expandersConfig,
                    injector, false, serializedKey, insName, labelName, weightName);
        }
        OptimizationMethod method = (OptimizationMethod) PredictorUtilities
                .getLearningMethod(methodConfig, injector, requestContext);
        method.minimize(model, data, valid);
        modelService.setModel(engineName, modelName, model);
        return model;
    }

    private SVDFeatureModel getModel(ModelService modelService, String engineName,
                                     RequestContext requestContext) {
        if (modelService.hasModel(engineName, modelName)) {
            return (SVDFeatureModel) modelService.getModel(engineName, modelName);
        } else {
            SVDFeatureModel ownModel = createNewModel(modelService, engineName, requestContext);
            modelService.setModel(engineName, modelName, ownModel);
            return ownModel;
        }
    }

    public Predictor getPredictor(RequestContext requestContext) {
        String engineName = requestContext.getEngineName();
        if (dependPredictorName != null) {
            SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
            configService.getPredictor(dependPredictorName, requestContext);
        }
        ModelService modelService = injector.instanceOf(ModelService.class);
        JsonNode reqBody = requestContext.getRequestBody();
        boolean toReset = IOUtilities.whetherModelOperation(modelName, ModelOperation.RESET, reqBody);
        if (toReset) {
            modelService.removeModel(engineName, modelName);
        }
        boolean toBuild = IOUtilities.whetherModelOperation(modelName, ModelOperation.BUILD, reqBody);
        SVDFeatureModel model;
        if (toBuild) {
            model = buildModel(requestContext, modelService, engineName);
        } else {
            model = getModel(modelService, engineName, requestContext);
        }
        boolean toLoad = IOUtilities.whetherModelOperation(modelName, ModelOperation.LOAD, reqBody);
        if (toLoad && dependPredictorName == null) {
            model = (SVDFeatureModel) RetrieverUtilities.loadModel(modelService, engineName,
                    modelName, modelFile);
        }
        boolean toUpdate = IOUtilities.whetherModelOperation(modelName, ModelOperation.UPDATE, reqBody);
        if (toUpdate) {
            updateModel(model, requestContext);
        }
        boolean toDump = IOUtilities.whetherModelOperation(modelName, ModelOperation.DUMP, reqBody);
        if (toDump && dependPredictorName == null) {
            RetrieverUtilities.dumpModel(model, modelFile);
        }
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        return new PredictiveModelBasedPredictor(config, model, model,
                entityDaoConfigs, injector, entityExpanders, daoConfigKey);
    }
}
