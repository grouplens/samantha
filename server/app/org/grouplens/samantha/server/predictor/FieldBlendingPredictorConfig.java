package org.grouplens.samantha.server.predictor;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class FieldBlendingPredictorConfig implements PredictorConfig {
    private final Object2DoubleMap<String> defaults;
    private final Injector injector;
    private final Configuration daoConfigs;
    private final Configuration config;
    private final List<Configuration> expandersConfig;
    private final String daoConfigKey;

    private FieldBlendingPredictorConfig(Configuration config, Configuration daoConfigs, Injector injector,
                                         List<Configuration> expandersConfig,
                                         Object2DoubleMap<String> defaults,
                                         String daoConfigKey) {
        this.config = config;
        this.daoConfigs = daoConfigs;
        this.defaults = defaults;
        this.expandersConfig = expandersConfig;
        this.injector = injector;
        this.daoConfigKey = daoConfigKey;
    }

    public static PredictorConfig getPredictorConfig(Configuration predictorConfig,
                                                     Injector injector) {
        Object2DoubleMap<String> defaults = new Object2DoubleOpenHashMap<>();
        Configuration defaultConfig = predictorConfig.getConfig("blendingDefaults");
        for (String key : defaultConfig.keys()) {
            defaults.put(key, defaultConfig.getDouble(key));
        }
        List<Configuration> expanders = ExpanderUtilities.getEntityExpandersConfig(predictorConfig);
        return new FieldBlendingPredictorConfig(predictorConfig,
                predictorConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get()),
                injector, expanders, defaults, predictorConfig.getString("daoConfigKey"));
    }

    public Predictor getPredictor(RequestContext requestContext) {
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        return new FieldBlendingPredictor(config, daoConfigs, injector, entityExpanders, defaults, daoConfigKey);
    }
}
