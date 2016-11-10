package org.grouplens.samantha.server.featurizer;

import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.featurizer.LogarithmicExtractor;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class LogarithmicExtractorConfig implements FeatureExtractorConfig {
    private final String indexName;
    private final String attrName;
    private final String feaName;

    private LogarithmicExtractorConfig(String indexName,
                                       String attrName,
                                       String feaName) {
        this.indexName = indexName;
        this.attrName = attrName;
        this.feaName = feaName;
    }

    public FeatureExtractor getFeatureExtractor(RequestContext requestContext) {
        return new LogarithmicExtractor(indexName, attrName, feaName);
    }

    public static FeatureExtractorConfig
    getFeatureExtractorConfig(Configuration extractorConfig,
                              Injector injector) {
        return new LogarithmicExtractorConfig(
                extractorConfig.getString("indexName"),
                extractorConfig.getString("attrName"),
                extractorConfig.getString("feaName")
        );
    }
}
