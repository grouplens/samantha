package org.grouplens.samantha.server.predictor;

import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractPredictor implements Predictor {
    protected final Configuration config;
    protected final Configuration daoConfigs;
    protected final Injector injector;
    private final String daoConfigKey;
    private final boolean verbose;

    public AbstractPredictor(Configuration config, Configuration daoConfigs,
                             String daoConfigKey, Injector injector) {
        this.config = config;
        this.daoConfigKey = daoConfigKey;
        this.daoConfigs = daoConfigs;
        this.injector = injector;
        Boolean verboseConfig = config.getBoolean(ConfigKey.ENGINE_COMPONENT_VERBOSE.get());
        if (verboseConfig != null) {
            this.verbose = verboseConfig;
        } else {
            this.verbose = false;
        }
    }

    public List<Prediction> predict(RequestContext requestContext) {
        return PredictorUtilities.predictFromRequest(this, requestContext, injector, daoConfigs, daoConfigKey);
    }

    public Configuration getConfig() {
        if (verbose) {
            return config;
        } else {
            Map<String, Object> configMap = new HashMap<>();
            configMap.put(ConfigKey.ENGINE_COMPONENT_NAME.get(),
                    config.getString(ConfigKey.ENGINE_COMPONENT_NAME.get()));
            return new Configuration(configMap);
        }
    }
}
