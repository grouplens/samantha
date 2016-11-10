package org.grouplens.samantha.server.featurizer;

import org.grouplens.samantha.modeler.featurizer.DecisionTreeLeafExtractor;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.tree.DecisionTree;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class DecisionTreeLeafExtractorConfig implements FeatureExtractorConfig {
    private final Injector injector;
    private final String predictorName;
    private final String modelName;
    private final String indexName;
    private final String feaName;

    private DecisionTreeLeafExtractorConfig(Injector injector,
                                            String predictorName,
                                            String modelName,
                                            String indexName,
                                            String feaName) {
        this.injector = injector;
        this.predictorName = predictorName;
        this.indexName = indexName;
        this.feaName = feaName;
        this.modelName = modelName;
    }

    public static FeatureExtractorConfig getFeatureExtractorConfig(Configuration extractorConfig,
                                                                   Injector injector) {
        return new DecisionTreeLeafExtractorConfig(injector,
                extractorConfig.getString("predictorName"),
                extractorConfig.getString("modelName"),
                extractorConfig.getString("indexName"),
                extractorConfig.getString("feaName"));
    }

    public FeatureExtractor getFeatureExtractor(RequestContext requestContext) {
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        ModelService modelService = injector.instanceOf(ModelService.class);
        configService.getPredictor(predictorName, requestContext);
        DecisionTree decisionTree = (DecisionTree) modelService.getModel(requestContext.getEngineName(),
                modelName);
        return new DecisionTreeLeafExtractor(decisionTree, feaName, indexName, requestContext);
    }
}
