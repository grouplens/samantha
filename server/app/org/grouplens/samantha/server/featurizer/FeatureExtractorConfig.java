package org.grouplens.samantha.server.featurizer;

import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public interface FeatureExtractorConfig {
    FeatureExtractor getFeatureExtractor(RequestContext requestContext);
    static FeatureExtractorConfig getFeatureExtractorConfig(Configuration predictorConfig,
                                              Injector injector) {return null;}
}
