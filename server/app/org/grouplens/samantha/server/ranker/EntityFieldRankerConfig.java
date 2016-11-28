package org.grouplens.samantha.server.ranker;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class EntityFieldRankerConfig implements RankerConfig {
    private final int pageSize;
    private final String orderField;
    private final boolean whetherOrder;
    private final boolean ascending;
    private final Configuration config;

    private EntityFieldRankerConfig(Configuration config, int pageSize,
                                    String orderField, boolean whetherOrder, boolean ascending) {
        this.pageSize = pageSize;
        this.orderField = orderField;
        this.whetherOrder = whetherOrder;
        this.ascending = ascending;
        this.config = config;
    }

    public static RankerConfig getRankerConfig(Configuration rankerConfig,
                                               Injector injector) {
        int pageSize = 24;
        if (rankerConfig.asMap().containsKey(ConfigKey.RANKER_PAGE_SIZE.get())) {
            pageSize = rankerConfig.getInt(ConfigKey.RANKER_PAGE_SIZE.get());
        }
        boolean whetherOrder = true;
        if (rankerConfig.asMap().containsKey("whetherOrder")) {
            whetherOrder = rankerConfig.getBoolean("whetherOrder"); 
        }
        return new EntityFieldRankerConfig(rankerConfig, pageSize, rankerConfig.getString("orderField"),
                whetherOrder, rankerConfig.getBoolean("ascending"));
    }

    public Ranker getRanker(RequestContext requestContext) {
        JsonNode requestBody = requestContext.getRequestBody();
        int page = JsonHelpers.getOptionalInt(requestBody, ConfigKey.RANKER_PAGE.get(), 1);
        int offset = JsonHelpers.getOptionalInt(requestBody,
                ConfigKey.RANKER_OFFSET.get(), (page - 1) * pageSize);
        int limit = JsonHelpers.getOptionalInt(requestBody, ConfigKey.RANKER_LIMIT.get(), pageSize);
        return new EntityFieldRanker(config, offset, limit, pageSize,
                whetherOrder, orderField, ascending);
    }
}
