package org.grouplens.samantha.server.featurizer;

import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.featurizer.NegativeStringValueExtractor;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class NegativeStringValueExtractorConfig implements FeatureExtractorConfig {
    private final String indexName;
    private final String feaName;
    private final String attrName;
    private final String toReplace;

    private NegativeStringValueExtractorConfig(String indexName,
                                               String attrName,
                                               String feaName,
                                               String toReplace) {
        this.indexName = indexName;
        this.attrName = attrName;
        this.feaName = feaName;
        this.toReplace = toReplace;
    }

    public FeatureExtractor getFeatureExtractor(RequestContext requestContext) {
        return new NegativeStringValueExtractor(indexName, attrName, feaName, toReplace);
    }

    public static FeatureExtractorConfig
    getFeatureExtractorConfig(Configuration extractorConfig,
                              Injector injector) {
        return new NegativeStringValueExtractorConfig(
                extractorConfig.getString("indexName"),
                extractorConfig.getString("attrName"),
                extractorConfig.getString("feaName"),
                extractorConfig.getString("toReplace")
        );
    }
}
