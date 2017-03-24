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

package org.grouplens.samantha.server.common;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.ImplementedBy;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@ImplementedBy(RedisLettuceService.class)
public interface RedisService {
    static String composeKey(String prefix, String key) {
        return prefix + "\1" + key;
    }

    static String composeKey(JsonNode entity, List<String> keyAttrs) {
        List<String> keys = new ArrayList<>();
        for (String attr : keyAttrs) {
            keys.add(composeKey(attr, entity.get(attr).asText()));
        }
        return StringUtils.join(keys, "\t");
    }

    void watch(String prefix, String key);
    void multi(boolean lock);
    List<Object> exec();
    String get(String prefix, String key);
    Long incre(String prefix, String key);
    Long increWithoutLock(String prefix, String key);
    String set(String prefix, String key, String value);
    String setWithoutLock(String prefix, String key, String value);
    void del(String prefix, String key);
    void delWithKey(String key);
    JsonNode getValue(String prefix, String key);
    void setValue(String prefix, String key, JsonNode value);
    List<String> keysWithPrefixPattern(String prefix, String key);
    List<JsonNode> bulkGet(List<String> keys);
    void indexIntoSortedSet(String prefix, String key, String scoreAttr, JsonNode data);
    void bulkIndexIntoSortedSet(String prefix, List<String> keyAttrs, String scoreAttr, JsonNode data);
    void bulkIndexIntoHashSet(String prefix, List<String> keyAttrs, List<String> hashAttrs, JsonNode data);
    List<JsonNode> bulkGetFromHashSet(String prefix, List<String> keyAttrs, JsonNode data);
    List<JsonNode> bulkUniqueGetFromHashSet(String prefix, List<String> keyAttrs, List<ObjectNode> data);
}
