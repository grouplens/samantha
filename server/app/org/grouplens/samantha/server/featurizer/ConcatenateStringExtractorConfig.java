package org.grouplens.samantha.server.featurizer;

import org.grouplens.samantha.modeler.featurizer.ConcatenateStringExtractor;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class ConcatenateStringExtractorConfig implements FeatureExtractorConfig {
    private final String indexName;
    private final String feaName;
    private final List<String> attrNames;

    private ConcatenateStringExtractorConfig(String indexName,
                                             List<String> attrNames,
                                             String feaName) {
        this.indexName = indexName;
        this.attrNames = attrNames;
        this.feaName = feaName;
    }

    public FeatureExtractor getFeatureExtractor(RequestContext requestContext) {
        return new ConcatenateStringExtractor(indexName, attrNames, feaName);
    }

    public static FeatureExtractorConfig
    getFeatureExtractorConfig(Configuration extractorConfig,
                              Injector injector) {
        return new ConcatenateStringExtractorConfig(
                extractorConfig.getString("indexName"),
                extractorConfig.getStringList("attrNames"),
                extractorConfig.getString("feaName")
        );
    }
}
