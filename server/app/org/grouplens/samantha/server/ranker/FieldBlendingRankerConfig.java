package org.grouplens.samantha.server.ranker;

import com.fasterxml.jackson.databind.JsonNode;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class FieldBlendingRankerConfig implements RankerConfig {
    private final int pageSize;
    private final Injector injector;
    private final Object2DoubleMap<String> defaults;
    private final List<Configuration> expandersConfig;
    private final Configuration config;

    private FieldBlendingRankerConfig(int pageSize, Object2DoubleMap<String> defaults,
                                      List<Configuration> expandersConfig, Injector injector,
                                      Configuration config) {
        this.pageSize = pageSize;
        this.defaults = defaults;
        this.expandersConfig = expandersConfig;
        this.injector = injector;
        this.config = config;
    }

    public static RankerConfig getRankerConfig(Configuration rankerConfig,
                                               Injector injector) {
        int pageSize = 24;
        if (rankerConfig.asMap().containsKey(ConfigKey.RANKER_PAGE_SIZE.get())) {
            rankerConfig.getInt(ConfigKey.RANKER_PAGE_SIZE.get());
        }
        Object2DoubleMap<String> defaults = new Object2DoubleOpenHashMap<>();
        Configuration defaultConfig = rankerConfig.getConfig("blendingDefaults");
        for (String key : defaultConfig.keys()) {
            defaults.put(key, defaultConfig.getDouble(key));
        }
        List<Configuration> expanders = ExpanderUtilities.getEntityExpandersConfig(rankerConfig);
        return new FieldBlendingRankerConfig(pageSize, defaults, expanders, injector, rankerConfig);
    }

    public Ranker getRanker(RequestContext requestContext) {
        JsonNode requestBody = requestContext.getRequestBody();
        int page = JsonHelpers.getOptionalInt(requestBody, ConfigKey.RANKER_PAGE.get(), 1);
        int offset = JsonHelpers.getOptionalInt(requestBody,
                ConfigKey.RANKER_OFFSET.get(), (page - 1) * pageSize);
        int limit = JsonHelpers.getOptionalInt(requestBody, ConfigKey.RANKER_LIMIT.get(), pageSize);
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        return new FieldBlendingRanker(defaults, offset, limit, pageSize, entityExpanders, config);
    }
}
