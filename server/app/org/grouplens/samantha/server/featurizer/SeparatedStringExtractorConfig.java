package org.grouplens.samantha.server.featurizer;

import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.featurizer.SeparatedStringExtractor;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class SeparatedStringExtractorConfig implements FeatureExtractorConfig {
    private final String indexName;
    private final String feaName;
    private final String attrName;
    private final String separator;

    private SeparatedStringExtractorConfig(String indexName,
                                           String attrName,
                                           String feaName,
                                           String separator) {
        this.indexName = indexName;
        this.attrName = attrName;
        this.feaName = feaName;
        this.separator = separator;
    }

    public FeatureExtractor getFeatureExtractor(RequestContext requestContext) {
        return new SeparatedStringExtractor(indexName, attrName, feaName, separator);
    }

    public static FeatureExtractorConfig
            getFeatureExtractorConfig(Configuration extractorConfig,
                                      Injector injector) {
        return new SeparatedStringExtractorConfig(
                extractorConfig.getString("indexName"),
                extractorConfig.getString("attrName"),
                extractorConfig.getString("feaName"),
                extractorConfig.getString("separator")
        );
    }
}
