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

package org.grouplens.samantha.modeler.metric;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.server.config.ConfigKey;
import play.libs.Json;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MetricUtilities {

    static public MetricResult getTopNResults(String metricName,
                                              List<Integer> N, Double threshold, double minValue,
                                              DoubleList metrics, int cnt) {
        List<ObjectNode> results = new ArrayList<>(N.size());
        ObjectNode metricPara = Json.newObject();
        if (threshold != null) {
            metricPara.put("threshold", threshold);
        }
        metricPara.put("minValue", minValue);
        boolean pass = true;
        for (int i=0; i<N.size(); i++) {
            ObjectNode result = Json.newObject();
            result.put(ConfigKey.EVALUATOR_METRIC_NAME.get(), metricName);
            metricPara.put("N", N.get(i));
            result.set(ConfigKey.EVALUATOR_METRIC_PARA.get(), metricPara);
            double value = 0.0;
            if (cnt > 0) {
                value = metrics.getDouble(i) / cnt;
            }
            result.put(ConfigKey.EVALUATOR_METRIC_VALUE.get(), value);
            results.add(result);
            if (value < minValue) {
                pass = false;
            }
        }
        return new MetricResult(results, pass);
    }

    static public Object2DoubleMap<String> getRelevantItemsWithValues(
            List<String> itemKeys, String separator,
            String relevanceKey, List<ObjectNode> groundTruth) {
        Object2DoubleMap<String> releItems = new Object2DoubleOpenHashMap<>();
        for (JsonNode entity : groundTruth) {
            if (separator != null) {
                List<String[]> values = new ArrayList<>();
                for (String itemKey : itemKeys) {
                    String[] splitted = entity.get(itemKey).asText().split(separator, -1);
                    values.add(splitted);
                }
                String[] relevance = entity.get(relevanceKey).asText().split(separator, -1);
                for (int i=0; i<relevance.length; i++) {
                    List<String> keys = new ArrayList<>();
                    for (String[] value : values) {
                        keys.add(value[i]);
                    }
                    String item = FeatureExtractorUtilities.composeKey(keys);
                    releItems.put(item, Double.parseDouble(relevance[i]));
                }
            } else {
                String item = FeatureExtractorUtilities.composeConcatenatedKeyWithoutName(entity, itemKeys);
                releItems.put(item, entity.get(relevanceKey).asDouble());
            }
        }
        return releItems;
    }

    static public Set<String> getRelevantItems(
            List<String> itemKeys, String separator,
            String relevanceKey, double threshold, List<ObjectNode> groundTruth) {
        Set<String> releItems = new HashSet<>();
        for (JsonNode entity : groundTruth) {
            if (separator != null) {
                List<String[]> values = new ArrayList<>();
                int size = 0;
                for (String itemKey : itemKeys) {
                    String[] splitted = entity.get(itemKey).asText().split(separator, -1);
                    size = splitted.length;
                    values.add(splitted);
                }
                String[] relevance = null;
                if (relevanceKey != null) {
                    relevance = entity.get(relevanceKey).asText().split(separator, -1);
                    size = relevance.length;
                }
                for (int i=0; i<size; i++) {
                    if (relevance != null && Double.parseDouble(relevance[i]) < threshold) {
                        continue;
                    }
                    List<String> keys = new ArrayList<>();
                    for (String[] value : values) {
                        keys.add(value[i]);
                    }
                    String item = FeatureExtractorUtilities.composeKey(keys);
                    releItems.add(item);
                }
            } else {
                if (relevanceKey == null || entity.get(relevanceKey).asDouble() >= threshold) {
                    String item = FeatureExtractorUtilities.composeConcatenatedKeyWithoutName(entity, itemKeys);
                    releItems.add(item);
                }
            }
        }
        return releItems;
    }
}
