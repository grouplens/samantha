package org.grouplens.samantha.server.featurizer;

import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.featurizer.SelfPlusOneRatioExtractor;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class SelfPlusOneRatioExtractorConfig implements FeatureExtractorConfig {
    private final String indexName;
    private final String attrName;
    private final String feaName;
    private final boolean sparse;
    private final boolean log;

    private SelfPlusOneRatioExtractorConfig(String indexName,
                                            String attrName,
                                            String feaName, boolean sparse, boolean log) {
        this.indexName = indexName;
        this.attrName = attrName;
        this.feaName = feaName;
        this.sparse = sparse;
        this.log = log;
    }

    public FeatureExtractor getFeatureExtractor(RequestContext requestContext) {
        return new SelfPlusOneRatioExtractor(indexName, attrName, feaName, sparse, log);
    }

    public static FeatureExtractorConfig
    getFeatureExtractorConfig(Configuration extractorConfig,
                              Injector injector) {
        boolean sparse = false;
        Boolean spa = extractorConfig.getBoolean("sparse");
        if (spa != null) {
            sparse = spa;
        }
        boolean log = false;
        Boolean logB = extractorConfig.getBoolean("log");
        if (logB != null) {
            log = logB;
        }
        return new SelfPlusOneRatioExtractorConfig(
                extractorConfig.getString("indexName"),
                extractorConfig.getString("attrName"),
                extractorConfig.getString("feaName"),
                sparse, log
        );
    }
}
