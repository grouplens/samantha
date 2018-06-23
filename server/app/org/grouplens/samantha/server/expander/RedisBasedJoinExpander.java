/*
 * Copyright (c) [2016-2018] [University of Minnesota]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.common.RedisService;
import org.grouplens.samantha.server.common.Utilities;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RedisBasedJoinExpander implements EntityExpander {
    private static Logger logger = LoggerFactory.getLogger(RedisBasedJoinExpander.class);
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
                String key = Utilities.composeKey(entity, keys);
                if (key2val.containsKey(key)) {
                    key2val.get(key).add(entity);
                } else {
                    List<JsonNode> list = new ArrayList<>();
                    list.add(entity);
                    key2val.put(key, list);
                }
            }
            for (ObjectNode entity : initialResult) {
                if (!Utilities.checkKeyAttributesComplete(entity, keys)) {
                    continue;
                }
                String key = Utilities.composeKey(entity, keys);
                if (key2val.containsKey(key)) {
                    for (JsonNode val : key2val.get(key)) {
                        IOUtilities.parseEntityFromJsonNode(entityFields, val, entity);
                    }
                } else {
                    logger.warn("Can not find the key {} for {} while joining.", key, entity.toString());
                }
            }
        }
        return initialResult;
    }
}
