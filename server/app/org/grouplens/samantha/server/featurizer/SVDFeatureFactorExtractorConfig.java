package org.grouplens.samantha.server.featurizer;

import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.featurizer.SVDFeatureFactorExtractor;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SVDFeatureFactorExtractorConfig implements FeatureExtractorConfig {
    private final Injector injector;
    private final String predictorName;
    private final String indexName;
    private final String modelName;
    private final Map<String, List<String>> fea2svdfeas;
    private final Boolean sparse;

    private SVDFeatureFactorExtractorConfig(Injector injector, Map<String, List<String>> fea2svdfeas,
                                            String indexName,
                                            String predictorName,
                                            String modelName, Boolean sparse) {
        this.injector = injector;
        this.predictorName = predictorName;
        this.modelName = modelName;
        this.indexName = indexName;
        this.fea2svdfeas = fea2svdfeas;
        this.sparse = sparse;
    }

    public static FeatureExtractorConfig getFeatureExtractorConfig(Configuration extractorConfig,
                                                            Injector injector) {
        Map<String, List<String>> fea2svdfeas = new HashMap<>();
        Configuration config = extractorConfig.getConfig("feature2dependents");
        for (String key : config.keys()) {
            fea2svdfeas.put(key, config.getStringList(key));
        }
        Boolean sparse = false;
        if (extractorConfig.asMap().containsKey("sparse")) {
            sparse = extractorConfig.getBoolean("sparse");
        }
        return new SVDFeatureFactorExtractorConfig(injector,
                fea2svdfeas,
                extractorConfig.getString("indexName"),
                extractorConfig.getString("predictorName"),
                extractorConfig.getString("modelName"), sparse);
    }

    public FeatureExtractor getFeatureExtractor(RequestContext requestContext) {
        SamanthaConfigService configService = injector
                .instanceOf(SamanthaConfigService.class);
        ModelService modelService = injector.instanceOf(ModelService.class);
        configService.getPredictor(predictorName, requestContext);
        SVDFeature model = (SVDFeature) modelService.getModel(requestContext.getEngineName(),
                modelName);
        return new SVDFeatureFactorExtractor(model, fea2svdfeas, sparse, indexName);
    }
}
