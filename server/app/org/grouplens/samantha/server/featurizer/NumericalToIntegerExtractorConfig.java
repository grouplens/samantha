package org.grouplens.samantha.server.featurizer;

import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.featurizer.NumericalToIntegerExtractor;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class NumericalToIntegerExtractorConfig implements FeatureExtractorConfig {
    private final String indexName;
    private final String feaName;
    private final String attrName;
    private final double multiplier;

    private NumericalToIntegerExtractorConfig(String indexName,
                                              String attrName,
                                              String feaName,
                                              double multiplier) {
        this.indexName = indexName;
        this.attrName = attrName;
        this.feaName = feaName;
        this.multiplier = multiplier;
    }

    public FeatureExtractor getFeatureExtractor(RequestContext requestContext) {
        return new NumericalToIntegerExtractor(indexName, attrName, feaName, multiplier);
    }

    public static FeatureExtractorConfig
    getFeatureExtractorConfig(Configuration extractorConfig,
                              Injector injector) {
        double multiplier = 1.0;
        if (extractorConfig.asMap().containsKey("multiplier")) {
            multiplier = extractorConfig.getDouble("multiplier");
        }
        return new NumericalToIntegerExtractorConfig(
                extractorConfig.getString("indexName"),
                extractorConfig.getString("attrName"),
                extractorConfig.getString("feaName"),
                multiplier
        );
    }
}
