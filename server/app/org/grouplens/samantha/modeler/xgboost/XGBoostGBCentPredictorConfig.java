package org.grouplens.samantha.modeler.xgboost;

import com.fasterxml.jackson.databind.JsonNode;

import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningMethod;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureModel;
import org.grouplens.samantha.server.common.ModelOperation;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.featurizer.FeatureExtractorConfig;
import org.grouplens.samantha.server.featurizer.FeatureExtractorListConfigParser;
import org.grouplens.samantha.server.featurizer.FeaturizerConfigParser;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.*;
import org.grouplens.samantha.server.retriever.RetrieverUtilities;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

public class XGBoostGBCentPredictorConfig implements PredictorConfig {
    private final String svdfeaPredictorName;
    private final String svdfeaModelName;
    private final Injector injector;
    private final String modelName;
    private final String modelFile;
    private final List<String> treeFeatures;
    private final List<FeatureExtractorConfig> treeExtractorsConfig;
    private final Configuration daosConfig;
    private final List<Configuration> expandersConfig;
    private final Configuration methodConfig;
    private final String daoConfigKey;
    private final String serializedKey;
    private final String insName;
    private final String labelName;
    private final String weightName;

    private XGBoostGBCentPredictorConfig(String modelName, String svdfeaModelName, String svdfeaPredictorName,
                                         List<String> treeFeatures, List<FeatureExtractorConfig> treeExtractorsConfig,
                                         Configuration daosConfig, List<Configuration> expandersConfig,
                                         Configuration methodConfig, Injector injector, String modelFile,
                                         String daoConfigKey, String insName, String labelName,
                                         String weightName, String serializedKey) {
        this.daosConfig = daosConfig;
        this.expandersConfig = expandersConfig;
        this.modelName = modelName;
        this.svdfeaModelName = svdfeaModelName;
        this.svdfeaPredictorName = svdfeaPredictorName;
        this.injector = injector;
        this.methodConfig = methodConfig;
        this.treeExtractorsConfig = treeExtractorsConfig;
        this.treeFeatures = treeFeatures;
        this.modelFile = modelFile;
        this.daoConfigKey = daoConfigKey;
        this.labelName = labelName;
        this.weightName = weightName;
        this.serializedKey = serializedKey;
        this.insName = insName;
    }

    public static PredictorConfig getPredictorConfig(Configuration predictorConfig,
                                                     Injector injector) {
        FeaturizerConfigParser parser = injector.instanceOf(
                FeatureExtractorListConfigParser.class);
        Configuration daosConfig = predictorConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get());
        List<FeatureExtractorConfig> feaExtConfigs = parser.parse(predictorConfig
                .getConfig(ConfigKey.PREDICTOR_FEATURIZER_CONFIG.get()));
        List<Configuration> expanders = ExpanderUtilities.getEntityExpandersConfig(predictorConfig);
        return new XGBoostGBCentPredictorConfig(predictorConfig.getString("modelName"),
                predictorConfig.getString("svdfeaModelName"), predictorConfig.getString("svdfeaPredictorName"),
                predictorConfig.getStringList("treeFeatures"), feaExtConfigs, daosConfig, expanders,
                predictorConfig.getConfig("methodConfig"), injector, predictorConfig.getString("modelFile"),
                predictorConfig.getString("daoConfigKey"), predictorConfig.getString("instanceName"),
                predictorConfig.getString("labelName"), predictorConfig.getString("weightName"),
                predictorConfig.getString("serializedKey"));
    }

    private XGBoostGBCent createNewModel(SamanthaConfigService configService,
                                         ModelService modelService,
                                         RequestContext requestContext) {
        List<FeatureExtractor> featureExtractors = new ArrayList<>();
        for (FeatureExtractorConfig feaExtConfig : treeExtractorsConfig) {
            featureExtractors.add(feaExtConfig.getFeatureExtractor(requestContext));
        }
        configService.getPredictor(svdfeaPredictorName, requestContext);
        SVDFeatureModel svdfeaModel = (SVDFeatureModel) modelService.getModel(requestContext.getEngineName(),
                svdfeaModelName);
        XGBoostGBCentProducer producer = injector.instanceOf(XGBoostGBCentProducer.class);
        XGBoostGBCent model = producer.createGBCentWithSVDFeatureModel(modelName, treeFeatures,
                featureExtractors, svdfeaModel);
        return model;
    }

    private XGBoostGBCent getModel(SamanthaConfigService configService,
                                          ModelService modelService, RequestContext requestContext) {
        String engineName = requestContext.getEngineName();
        if (modelService.hasModel(engineName, modelName)) {
            return (XGBoostGBCent) modelService.getModel(engineName, modelName);
        } else {
            XGBoostGBCent model = createNewModel(configService, modelService, requestContext);
            modelService.setModel(engineName, modelName, model);
            return model;
        }
    }

    private XGBoostGBCent buildModel(SamanthaConfigService configService,
                                            RequestContext requestContext,
                                            ModelService modelService) {
        JsonNode reqBody = requestContext.getRequestBody();
        XGBoostGBCent model = createNewModel(configService, modelService, requestContext);
        LearningData data = PredictorUtilities.getLearningData(model, requestContext,
                reqBody.get("learningDaoConfig"), daosConfig, expandersConfig, injector, true,
                serializedKey, insName, labelName, weightName);
        LearningData valid = null;
        if (reqBody.has("validationDaoConfig"))  {
            valid = PredictorUtilities.getLearningData(model, requestContext,
                    reqBody.get("validationDaoConfig"), daosConfig, expandersConfig, injector, false,
                    serializedKey, insName, labelName, weightName);
        }
        LearningMethod method = PredictorUtilities.getLearningMethod(methodConfig, injector, requestContext);
        method.learn(model, data, valid);
        modelService.setModel(requestContext.getEngineName(), modelName, model);
        return model;
    }

    public Predictor getPredictor(RequestContext requestContext) {
        ModelService modelService = injector.instanceOf(ModelService.class);
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        JsonNode reqBody = requestContext.getRequestBody();
        String engineName = requestContext.getEngineName();
        boolean toBuild = IOUtilities.whetherModelOperation(modelName, ModelOperation.BUILD, reqBody);
        XGBoostGBCent model;
        if (toBuild) {
            model = buildModel(configService, requestContext, modelService);
        } else {
            model = getModel(configService, modelService, requestContext);
        }
        boolean toLoad = IOUtilities.whetherModelOperation(modelName, ModelOperation.LOAD, reqBody);
        if (toLoad) {
            model = (XGBoostGBCent) RetrieverUtilities.loadModel(modelService, engineName, modelName,
                    modelFile);
        }
        boolean toDump = IOUtilities.whetherModelOperation(modelName, ModelOperation.DUMP, reqBody);
        if (toDump) {
            RetrieverUtilities.dumpModel(model, modelFile);
        }
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        //TODO: add model parameters into footprint
        JsonNode footprint = Json.newObject();
        return new PredictiveModelBasedPredictor(footprint, model, model,
                daosConfig, injector, entityExpanders, daoConfigKey);
    }
}
