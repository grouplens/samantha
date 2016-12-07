package org.grouplens.samantha.server.predictor;

import com.fasterxml.jackson.databind.JsonNode;

import org.grouplens.samantha.modeler.boosting.RegressionTreeGBCentProducer;
import org.grouplens.samantha.modeler.boosting.RegressionTreeGBCent;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningMethod;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.space.SpaceMode;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.server.common.AbstractModelManager;
import org.grouplens.samantha.server.common.ModelManager;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.featurizer.FeatureExtractorConfig;
import org.grouplens.samantha.server.featurizer.FeatureExtractorListConfigParser;
import org.grouplens.samantha.server.featurizer.FeaturizerConfigParser;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class RegressionTreeGBCentPredictorConfig implements PredictorConfig {
    private final String svdfeaPredictorName;
    private final String svdfeaModelName;
    private final Injector injector;
    private final String modelName;
    private final String modelFile;
    private final List<String> treeFeatures;
    private final List<String> groupKeys;
    private final List<FeatureExtractorConfig> treeExtractorsConfig;
    private final Configuration daosConfig;
    private final List<Configuration> expandersConfig;
    private final Configuration methodConfig;
    private final String daoConfigKey;
    private final String serializedKey;
    private final String insName;
    private final String labelName;
    private final String weightName;
    private final Configuration config;

    private RegressionTreeGBCentPredictorConfig(String modelName, String svdfeaModelName, String svdfeaPredictorName,
                                                List<String> treeFeatures, List<String> groupKeys,
                                                List<FeatureExtractorConfig> treeExtractorsConfig,
                                                Configuration daosConfig, List<Configuration> expandersConfig,
                                                Configuration methodConfig, Injector injector, String modelFile,
                                                String daoConfigKey, String serializedKey, String insName,
                                                String labelName, String weightName, Configuration config) {
        this.daosConfig = daosConfig;
        this.expandersConfig = expandersConfig;
        this.modelName = modelName;
        this.svdfeaModelName = svdfeaModelName;
        this.svdfeaPredictorName = svdfeaPredictorName;
        this.injector = injector;
        this.methodConfig = methodConfig;
        this.groupKeys = groupKeys;
        this.treeExtractorsConfig = treeExtractorsConfig;
        this.treeFeatures = treeFeatures;
        this.modelFile = modelFile;
        this.daoConfigKey = daoConfigKey;
        this.serializedKey = serializedKey;
        this.insName = insName;
        this.labelName = labelName;
        this.weightName = weightName;
        this.config = config;
    }

    public static PredictorConfig getPredictorConfig(Configuration predictorConfig,
                                                     Injector injector) {
        FeaturizerConfigParser parser = injector.instanceOf(
                FeatureExtractorListConfigParser.class);
        Configuration daosConfig = predictorConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get());
        List<FeatureExtractorConfig> feaExtConfigs = parser.parse(predictorConfig
                .getConfig(ConfigKey.PREDICTOR_FEATURIZER_CONFIG.get()));
        List<Configuration> expanders = ExpanderUtilities.getEntityExpandersConfig(predictorConfig);
        return new RegressionTreeGBCentPredictorConfig(predictorConfig.getString("modelName"),
                predictorConfig.getString("svdfeaModelName"), predictorConfig.getString("svdfeaPredictorName"),
                predictorConfig.getStringList("treeFeatures"), predictorConfig.getStringList("groupKeys"),
                feaExtConfigs, daosConfig, expanders,
                predictorConfig.getConfig("methodConfig"), injector, predictorConfig.getString("modelFile"),
                predictorConfig.getString("daoConfigKey"),
                predictorConfig.getString("instanceName"),
                predictorConfig.getString("serializedKey"),
                predictorConfig.getString("labelName"),
                predictorConfig.getString("weightName"), predictorConfig);
    }

    private class RegressionTreeGBCentModelManager extends AbstractModelManager {

        public RegressionTreeGBCentModelManager(String modelName, String modelFile, Injector injector) {
            super(injector, modelName, modelFile, new ArrayList<>());
        }

        public Object createModel(RequestContext requestContext, SpaceMode spaceMode) {
            List<FeatureExtractor> featureExtractors = new ArrayList<>();
            for (FeatureExtractorConfig feaExtConfig : treeExtractorsConfig) {
                featureExtractors.add(feaExtConfig.getFeatureExtractor(requestContext));
            }
            SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
            configService.getPredictor(svdfeaPredictorName, requestContext);
            ModelService modelService = injector.instanceOf(ModelService.class);
            SVDFeature svdfeaModel = (SVDFeature) modelService.getModel(requestContext.getEngineName(),
                    svdfeaModelName);
            RegressionTreeGBCentProducer producer = injector.instanceOf(RegressionTreeGBCentProducer.class);
            RegressionTreeGBCent model = producer.createGBCentWithSVDFeatureModel(modelName, spaceMode,
                    treeFeatures, featureExtractors, svdfeaModel);
            return model;
        }

        public Object buildModel(Object model, RequestContext requestContext) {
            JsonNode reqBody = requestContext.getRequestBody();
            RegressionTreeGBCent gbcent = (RegressionTreeGBCent) model;
            LearningData data = PredictorUtilities.getLearningData(gbcent, requestContext,
                    reqBody.get("learningDaoConfig"), daosConfig, expandersConfig, injector, true,
                    serializedKey, insName, labelName, weightName, groupKeys);
            LearningData valid = null;
            if (reqBody.has("validationDaoConfig"))  {
                valid = PredictorUtilities.getLearningData(gbcent, requestContext,
                        reqBody.get("validationDaoConfig"), daosConfig, expandersConfig, injector, false,
                        serializedKey, insName, labelName, weightName, groupKeys);
            }
            LearningMethod method = PredictorUtilities.getLearningMethod(methodConfig, injector, requestContext);
            method.learn(gbcent, data, valid);
            return model;
        }
    }

    public Predictor getPredictor(RequestContext requestContext) {
        ModelManager modelManager = new RegressionTreeGBCentModelManager(modelName, modelFile, injector);
        RegressionTreeGBCent model = (RegressionTreeGBCent) modelManager.manage(requestContext);
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        return new PredictiveModelBasedPredictor(config, model, model,
                daosConfig, injector, entityExpanders, daoConfigKey);
    }
}
