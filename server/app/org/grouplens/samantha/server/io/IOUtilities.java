package org.grouplens.samantha.server.io;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.common.ModelOperation;
import org.grouplens.samantha.server.config.ConfigKey;
import play.libs.Json;

import java.util.*;

public class IOUtilities {

    private IOUtilities() {}

    static public void parseEntityFromJsonNode(List<String> entityFields,
                                               JsonNode json, ObjectNode entity) {
        for (String fieldName : entityFields) {
            if (json.has(fieldName)) {
                JsonNode value = json.get(fieldName);
                entity.set(fieldName, value);
            }
        }
    }

    static public void parseEntityFromJsonNode(JsonNode json, ObjectNode entity) {
        parseEntityFromJsonNode(Lists.newArrayList(json.fieldNames()), json, entity);
    }

    static public void parseEntityFromMap(ObjectNode entity, Map<String, Object> objMap) {
        for (String fieldName : objMap.keySet()) {
            entity.set(fieldName, Json.toJson(objMap.get(fieldName)));
        }
    }

    static public Map<String, String> getKeyValueFromEntity(JsonNode entity, List<String> keys) {
        Map<String, String> keyVal = new HashMap<>(keys.size());
        for (String key : keys) {
            if (entity.has(key)) {
                keyVal.put(key, entity.get(key).asText());
            }
        }
        return keyVal;
    }
}
