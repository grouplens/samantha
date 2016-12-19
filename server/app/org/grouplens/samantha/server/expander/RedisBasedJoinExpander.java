package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.common.RedisService;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.Logger;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RedisBasedJoinExpander implements EntityExpander {
    final private List<Configuration> configList;
    final private RedisService redisService;

    public RedisBasedJoinExpander(List<Configuration> configList, RedisService redisService) {
        this.configList = configList;
        this.redisService = redisService;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector,
                                             RequestContext requestContext) {
        return new RedisBasedJoinExpander(expanderConfig.getConfigList("expandFields"),
                injector.instanceOf(RedisService.class));
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        for (Configuration config : configList) {
            String prefix = config.getString("prefix");
            List<String> keys = config.getStringList("keys");
            List<JsonNode> retrieved = redisService.bulkUniqueGetFromHashSet(prefix, keys, initialResult);
            List<String> entityFields = config.getStringList("fields");
            Map<String, List<JsonNode>> key2val = new HashMap<>();
            for (JsonNode entity : retrieved) {
                String key = RedisService.composeKey(entity, keys);
                if (key2val.containsKey(key)) {
                    key2val.get(key).add(entity);
                } else {
                    List<JsonNode> list = new ArrayList<>();
                    list.add(entity);
                    key2val.put(key, list);
                }
            }
            for (ObjectNode entity : initialResult) {
                String key = RedisService.composeKey(entity, keys);
                if (key2val.containsKey(key)) {
                    for (JsonNode val : key2val.get(key)) {
                        IOUtilities.parseEntityFromJsonNode(entityFields, val, entity);
                    }
                } else {
                    Logger.warn("Can not find the key for {} while joining.", entity.toString());
                }
            }
        }
        return initialResult;
    }
}
