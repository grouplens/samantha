package org.grouplens.samantha.server.predictor;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.tree.RegressionTree;
import org.grouplens.samantha.modeler.tree.RegressionTreeProducer;
import org.grouplens.samantha.modeler.tree.TreeLearningMethod;
import org.grouplens.samantha.server.common.ModelOperation;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.featurizer.FeatureExtractorConfig;
import org.grouplens.samantha.server.featurizer.FeatureExtractorListConfigParser;
import org.grouplens.samantha.server.featurizer.FeaturizerConfigParser;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.retriever.RetrieverUtilities;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

public class RegressionTreePredictorConfig implements PredictorConfig {
    private final String modelName;
    private final String modelFile;
    private final List<FeatureExtractorConfig> feaExtConfigs;
    private final List<String> features;
    private final String labelName;
    private final String weightName;
    private final Configuration daoConfigs;
    private final List<Configuration> expandersConfig;
    private final Injector injector;
    private final TreeLearningMethod method;
    private final String daoConfigKey;
    private final String serializedKey;
    private final String insName;
    private final Configuration config;

    private RegressionTreePredictorConfig(String modelName, List<FeatureExtractorConfig> feaExtConfigs,
                                          List<String> features, String labelName, String weightName,
                                          Configuration daoConfigs, List<Configuration> expandersConfig,
                                          Injector injector, TreeLearningMethod method, String modelFile,
                                          String daoConfigKey, String insName, String serializedKey,
                                          Configuration config) {
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
        return new RegressionTreePredictorConfig(predictorConfig.getString("modelName"),
                feaExtConfigs, predictorConfig.getStringList("features"),
                predictorConfig.getString("labelName"),
                predictorConfig.getString("weightName"), daoConfigs, expanders, injector,
                injector.instanceOf(TreeLearningMethod.class),
                predictorConfig.getString("modelFile"),
                predictorConfig.getString("daoConfigKey"),
                predictorConfig.getString("instanceName"),
                predictorConfig.getString("serializedKey"), predictorConfig);
    }

    private RegressionTree createNewModel(RequestContext requestContext) {
        List<FeatureExtractor> featureExtractors = new ArrayList<>();
        for (FeatureExtractorConfig feaExtConfig : feaExtConfigs) {
            featureExtractors.add(feaExtConfig.getFeatureExtractor(requestContext));
        }
        RegressionTreeProducer producer = injector.instanceOf(RegressionTreeProducer.class);
        RegressionTree model = producer.createRegressionTree(modelName, features, featureExtractors,
                labelName, weightName);
        return model;
    }

    private RegressionTree getModel(ModelService modelService, String engineName,
                                    RequestContext requestContext) {
        if (modelService.hasModel(engineName, modelName)) {
            return (RegressionTree) modelService.getModel(engineName, modelName);
        } else {
            RegressionTree model = createNewModel(requestContext);
            modelService.setModel(engineName, modelName, model);
            return model;
        }
    }

    private RegressionTree buildModel(RequestContext requestContext,
                                      ModelService modelService) {
        RegressionTree model = createNewModel(requestContext);
        LearningData data = PredictorUtilities.getLearningData(model, requestContext,
                requestContext.getRequestBody().get(daoConfigKey), daoConfigs, expandersConfig,
                injector, true, serializedKey, insName, labelName, weightName);
        method.learn(model, data, null);
        modelService.setModel(requestContext.getEngineName(), modelName, model);
        return model;
    }

    public Predictor getPredictor(RequestContext requestContext) {
        String engineName = requestContext.getEngineName();
        ModelService modelService = injector.instanceOf(ModelService.class);
        JsonNode reqBody = requestContext.getRequestBody();
        boolean toBuild = IOUtilities.whetherModelOperation(modelName, ModelOperation.BUILD, reqBody);
        RegressionTree model;
        if (toBuild) {
            model = buildModel(requestContext, modelService);
        } else {
            model = getModel(modelService, engineName, requestContext);
        }
        boolean toLoad = IOUtilities.whetherModelOperation(modelName, ModelOperation.LOAD, reqBody);
        if (toLoad) {
            model = (RegressionTree) RetrieverUtilities.loadModel(modelService, engineName, modelName,
                    modelFile);
        }
        boolean toDump = IOUtilities.whetherModelOperation(modelName, ModelOperation.DUMP, reqBody);
        if (toDump) {
            RetrieverUtilities.dumpModel(model, modelFile);
        }
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        return new PredictiveModelBasedPredictor(config, model, model,
                daoConfigs, injector, entityExpanders, daoConfigKey);
    }
}
