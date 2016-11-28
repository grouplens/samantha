package org.grouplens.samantha.server.predictor;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.boosting.GBDTProducer;
import org.grouplens.samantha.modeler.boosting.GBDT;
import org.grouplens.samantha.modeler.boosting.StandardBoostingMethod;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.solver.L2NormLoss;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
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

public class GBDTPredictorConfig implements PredictorConfig {
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
    private final StandardBoostingMethod boostingMethod;
    private final ObjectiveFunction objectiveFunction;
    private final String daoConfigKey;
    private final String serializedKey;
    private final String insName;
    private final Configuration config;

    private GBDTPredictorConfig(String modelName, List<FeatureExtractorConfig> feaExtConfigs,
                                List<String> features, String labelName, String weightName,
                                Configuration daoConfigs, List<Configuration> expandersConfig,
                                Injector injector, TreeLearningMethod method,
                                ObjectiveFunction objectiveFunction, String modelFile,
                                StandardBoostingMethod boostingMethod, String daoConfigKey,
                                String insName, String serializedKey, Configuration config) {
        this.modelName = modelName;
        this.feaExtConfigs = feaExtConfigs;
        this.features = features;
        this.labelName = labelName;
        this.weightName = weightName;
        this.daoConfigs = daoConfigs;
        this.expandersConfig = expandersConfig;
        this.injector = injector;
        this.method = method;
        this.objectiveFunction = objectiveFunction;
        this.modelFile = modelFile;
        this.boostingMethod = boostingMethod;
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
        int maxIter = predictorConfig.getInt("maxNumTrees");
        StandardBoostingMethod boostingMethod = new StandardBoostingMethod(maxIter);
        return new GBDTPredictorConfig(predictorConfig.getString("modelName"),
                feaExtConfigs, predictorConfig.getStringList("features"),
                predictorConfig.getString("labelName"),
                predictorConfig.getString("weightName"), daoConfigs, expanders, injector,
                injector.instanceOf(TreeLearningMethod.class), injector.instanceOf(L2NormLoss.class),
                predictorConfig.getString("modelFile"), boostingMethod,
                predictorConfig.getString("daoConfigKey"),
                predictorConfig.getString("instanceName"),
                predictorConfig.getString("serializedKey"), predictorConfig);
    }

    private class GBDTModelManager extends AbstractModelManager {

        public GBDTModelManager(String modelName, String modelFile, Injector injector) {
            super(injector, modelName, modelFile);
        }

        public Object createModel(RequestContext requestContext) {
            List<FeatureExtractor> featureExtractors = new ArrayList<>();
            for (FeatureExtractorConfig feaExtConfig : feaExtConfigs) {
                featureExtractors.add(feaExtConfig.getFeatureExtractor(requestContext));
            }
            GBDTProducer producer = injector.instanceOf(GBDTProducer.class);
            GBDT model = producer.createGBRT(modelName, objectiveFunction, method,
                    features, featureExtractors, labelName, weightName);
            return model;
        }

        public Object buildModel(Object model, RequestContext requestContext) {
            GBDT gbdt = (GBDT) model;
            JsonNode reqBody = requestContext.getRequestBody();
            LearningData data = PredictorUtilities.getLearningData(gbdt, requestContext,
                    reqBody.get("learningDaoConfig"), daoConfigs, expandersConfig, injector, true,
                    serializedKey, insName, labelName, weightName);
            LearningData valid = null;
            if (reqBody.has("validationDaoConfig"))  {
                valid = PredictorUtilities.getLearningData(gbdt, requestContext,
                        reqBody.get("validationDaoConfig"), daoConfigs, expandersConfig, injector, false,
                        serializedKey, insName, labelName, weightName);
            }
            boostingMethod.learn(gbdt, data, valid);
            return model;
        }
    }

    public Predictor getPredictor(RequestContext requestContext) {
        ModelManager modelManager = new GBDTModelManager(modelName, modelFile, injector);
        GBDT model = (GBDT) modelManager.manage(requestContext);
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        return new PredictiveModelBasedPredictor(config, model, model,
                daoConfigs, injector, entityExpanders, daoConfigKey);
    }
}
