package org.grouplens.samantha.server.featurizer;

import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.featurizer.PairwiseInteractionExtractor;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class PairwiseInteractionExtractorConfig implements FeatureExtractorConfig {
    private final String indexName;
    private final List<String> attrNames;
    private final boolean sigmoid;

    private PairwiseInteractionExtractorConfig(String indexName,
                                        List<String> attrNames,
                                        boolean sigmoid) {
        this.indexName = indexName;
        this.attrNames = attrNames;
        this.sigmoid = sigmoid;
    }

    public FeatureExtractor getFeatureExtractor(RequestContext requestContext) {
        return new PairwiseInteractionExtractor(indexName, attrNames, sigmoid);
    }

    public static FeatureExtractorConfig
    getFeatureExtractorConfig(Configuration extractorConfig,
                              Injector injector) {
        boolean sigmoid = false;
        if (extractorConfig.asMap().containsKey("sigmoid")) {
            sigmoid = extractorConfig.getBoolean("sigmoid");
        }
        return new PairwiseInteractionExtractorConfig(
                extractorConfig.getString("indexName"),
                extractorConfig.getStringList("attrNames"),
                sigmoid
        );
    }
}
