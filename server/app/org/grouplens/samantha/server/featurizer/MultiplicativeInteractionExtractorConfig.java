package org.grouplens.samantha.server.featurizer;

import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.featurizer.MultiplicativeInteractionExtractor;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class MultiplicativeInteractionExtractorConfig implements FeatureExtractorConfig {
    private final String indexName;
    private final String feaName;
    private final List<String> attrNames;
    private final boolean sigmoid;

    private MultiplicativeInteractionExtractorConfig(String indexName,
                                                     List<String> attrNames,
                                                     String feaName, boolean sigmoid) {
        this.indexName = indexName;
        this.attrNames = attrNames;
        this.feaName = feaName;
        this.sigmoid = sigmoid;
    }

    public FeatureExtractor getFeatureExtractor(RequestContext requestContext) {
        return new MultiplicativeInteractionExtractor(indexName, attrNames, feaName, sigmoid);
    }

    public static FeatureExtractorConfig
    getFeatureExtractorConfig(Configuration extractorConfig,
                              Injector injector) {
        boolean sigmoid = false;
        if (extractorConfig.asMap().containsKey("sigmoid")) {
            sigmoid = extractorConfig.getBoolean("sigmoid");
        }
        return new MultiplicativeInteractionExtractorConfig(
                extractorConfig.getString("indexName"),
                extractorConfig.getStringList("attrNames"),
                extractorConfig.getString("feaName"),
                sigmoid
        );
    }
}
