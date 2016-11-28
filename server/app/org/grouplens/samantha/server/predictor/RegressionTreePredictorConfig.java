package org.grouplens.samantha.server.predictor;

import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.tree.RegressionTree;
import org.grouplens.samantha.modeler.tree.RegressionTreeProducer;
import org.grouplens.samantha.modeler.tree.TreeLearningMethod;
import org.grouplens.samantha.server.common.AbstractModelManager;
import org.grouplens.samantha.server.common.ModelManager;
import org.grouplens.samantha.server.config.ConfigKey;
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
        List<Configuration> expanders = ExpanderUtilities.getEntityExpandersConfig(predictorConfig);
        List<FeatureExtractorConfig> feaExtConfigs = parser.parse(predictorConfig
                .getConfig(ConfigKey.PREDICTOR_FEATURIZER_CONFIG.get()));
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

    private class RegressionTreeModelManager extends AbstractModelManager {

        public RegressionTreeModelManager(String modelName, String modelFile, Injector injector) {
            super(injector, modelName, modelFile);
        }

        public Object createModel(RequestContext requestContext) {
            List<FeatureExtractor> featureExtractors = new ArrayList<>();
            for (FeatureExtractorConfig feaExtConfig : feaExtConfigs) {
                featureExtractors.add(feaExtConfig.getFeatureExtractor(requestContext));
            }
            RegressionTreeProducer producer = injector.instanceOf(RegressionTreeProducer.class);
            RegressionTree model = producer.createRegressionTree(modelName, features, featureExtractors,
                    labelName, weightName);
            return model;
        }

        public Object buildModel(Object model, RequestContext requestContext) {
            RegressionTree regressionTree = (RegressionTree) model;
            LearningData data = PredictorUtilities.getLearningData(regressionTree, requestContext,
                    requestContext.getRequestBody().get(daoConfigKey), daoConfigs, expandersConfig,
                    injector, true, serializedKey, insName, labelName, weightName);
            method.learn(regressionTree, data, null);
            return model;
        }
    }

    public Predictor getPredictor(RequestContext requestContext) {
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        ModelManager modelManager = new RegressionTreeModelManager(modelName, modelFile, injector);
        RegressionTree model = (RegressionTree) modelManager.manage(requestContext);
        return new PredictiveModelBasedPredictor(config, model, model,
                daoConfigs, injector, entityExpanders, daoConfigKey);
    }
}
