package org.grouplens.samantha.server.featurizer;

import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.exception.ConfigurationException;
import play.Configuration;
import play.inject.Injector;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class FeatureExtractorListConfigParser implements FeaturizerConfigParser {
    private final Injector injector;

    @Inject
    public FeatureExtractorListConfigParser(Injector injector) {
        this.injector = injector;
    }

    public List<FeatureExtractorConfig> parse(Configuration featurizerConfig)
            throws ConfigurationException {
        List<Configuration> configList = featurizerConfig
                .getConfigList(ConfigKey.BASIC_FEATURE_EXTRACTOR_LIST_CONFIG.get());
        List<FeatureExtractorConfig> extConfigs = new ArrayList<>(configList.size());
        try {
            for (Configuration config : configList) {
                String extractorConfigClass = config
                        .getString(ConfigKey.FEATURE_EXTRACTOR_CONFIG_CLASS.get());
                Method method = Class.forName(extractorConfigClass)
                        .getMethod("getFeatureExtractorConfig", Configuration.class,
                                Injector.class);
                FeatureExtractorConfig configInstance = (FeatureExtractorConfig)(method
                        .invoke(null, config, injector));
                extConfigs.add(configInstance);
            }
        } catch (IllegalAccessException | InvocationTargetException |
                ClassNotFoundException | NoSuchMethodException e) {
            throw new ConfigurationException(e);
        }
        return extConfigs;
    }
}
