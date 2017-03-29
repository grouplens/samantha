/*
 * Copyright (c) [2016-2017] [University of Minnesota]
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
