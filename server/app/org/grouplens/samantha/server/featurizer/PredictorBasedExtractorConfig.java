package org.grouplens.samantha.server.featurizer;

import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.Predictor;
import play.Configuration;
import play.inject.Injector;

/**
 * Use of this class is discouraged. Instead, use {@link org.grouplens.samantha.server.expander.PredictorBasedExpander
 * PredictorBasedExpander} together with a {@link IdentityExtractorConfig}.
 */
public class PredictorBasedExtractorConfig implements FeatureExtractorConfig {
    private final Injector injector;
    private final String predictorName;
    private final String indexName;
    private final String feaName;

    private PredictorBasedExtractorConfig(Injector injector,
                                          String predictorName,
                                          String indexName,
                                          String feaName) {
        this.injector = injector;
        this.predictorName = predictorName;
        this.indexName = indexName;
        this.feaName = feaName;
    }

    public static FeatureExtractorConfig getFeatureExtractorConfig(Configuration extractorConfig,
                                                            Injector injector) {
        return new PredictorBasedExtractorConfig(injector,
                extractorConfig.getString("predictorName"),
                extractorConfig.getString("indexName"),
                extractorConfig.getString("feaName"));
    }

    public FeatureExtractor getFeatureExtractor(RequestContext requestContext) {
        SamanthaConfigService configService = injector
                .instanceOf(SamanthaConfigService.class);
        Predictor predictor = configService.getPredictor(predictorName, requestContext);
        return new PredictorBasedExtractor(predictor, requestContext, feaName, indexName);
    }
}
