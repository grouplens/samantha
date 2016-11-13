package org.grouplens.samantha.server.ranker;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class EntityFieldBasedRankerConfig implements RankerConfig {
    private final int pageSize;
    private final String orderFieldKey;
    private final String whetherOrderKey;
    private final String ascendingKey;
    private final Configuration config;

    private EntityFieldBasedRankerConfig(Configuration config, int pageSize,
                                         String orderFieldKey, String whetherOrderKey, String ascendingKey) {
        this.pageSize = pageSize;
        this.orderFieldKey = orderFieldKey;
        this.whetherOrderKey = whetherOrderKey;
        this.ascendingKey = ascendingKey;
        this.config = config;
    }

    public static RankerConfig getRankerConfig(Configuration rankerConfig,
                                               Injector injector) {
        int pageSize = 24;
        if (rankerConfig.asMap().containsKey(ConfigKey.RANKER_PAGE_SIZE.get())) {
            rankerConfig.getInt(ConfigKey.RANKER_PAGE_SIZE.get());
        }
        return new EntityFieldBasedRankerConfig(rankerConfig, pageSize, rankerConfig.getString("orderFieldKey"),
                rankerConfig.getString("whetherOrderKey"), rankerConfig.getString("ascendingKey"));
    }

    public Ranker getRanker(RequestContext requestContext) {
        JsonNode requestBody = requestContext.getRequestBody();
        int page = JsonHelpers.getOptionalInt(requestBody, ConfigKey.RANKER_PAGE.get(), 1);
        int offset = JsonHelpers.getOptionalInt(requestBody,
                ConfigKey.RANKER_OFFSET.get(), (page - 1) * pageSize);
        int limit = JsonHelpers.getOptionalInt(requestBody, ConfigKey.RANKER_LIMIT.get(), pageSize);
        return new EntityFieldBasedRanker(config, offset, limit, pageSize,
                whetherOrderKey, orderFieldKey, ascendingKey);
    }
}
