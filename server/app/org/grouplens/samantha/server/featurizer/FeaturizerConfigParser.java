package org.grouplens.samantha.server.featurizer;

import org.grouplens.samantha.server.exception.ConfigurationException;
import play.Configuration;

import java.util.List;

public interface FeaturizerConfigParser {
    List<FeatureExtractorConfig> parse(Configuration config) throws ConfigurationException;
}
