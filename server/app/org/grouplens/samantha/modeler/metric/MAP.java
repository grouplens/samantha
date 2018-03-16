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
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.config.ConfigKey;
import play.Logger;
import play.libs.Json;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MAP implements Metric {
    private int cnt = 0;
    private DoubleList AP;
    private final List<Integer> N;
    private final List<String> itemKeys;
    private final String relevanceKey;
    private final double threshold;
    private final double minValue;

    public MAP(List<Integer> N, List<String> itemKeys, String relevanceKey,
               double threshold, double minValue) {
        this.N = N;
        this.itemKeys = itemKeys;
        this.relevanceKey = relevanceKey;
        this.threshold = threshold;
        this.minValue = minValue;
        this.AP = new DoubleArrayList(N.size());
        for (int i=0; i<N.size(); i++) {
            this.AP.add(0.0);
        }
    }

    public void add(List<ObjectNode> groundTruth, List<Prediction> recommendations) {
        Set<String> releItems = new HashSet<>();
        for (JsonNode entity : groundTruth) {
            if (relevanceKey == null || entity.get(relevanceKey).asDouble() >= threshold) {
                String item = FeatureExtractorUtilities.composeConcatenatedKey(entity, itemKeys);
                releItems.add(item);
            }
        }
        if (releItems.size() == 0) {
            return;
        }
        int maxN = 0;
        for (Integer n : N) {
            if (n > maxN) {
                maxN = n;
            }
            if (recommendations.size() < n) {
                Logger.error("The number of recommendations({}) is less than the indicated MAP N({})",
                        recommendations.size(), n);
            }
        }
        int hits = 0;
        double[] ap = new double[N.size()];
        for (int i=0; i<recommendations.size(); i++) {
            int rank = i + 1;
            String recItem = FeatureExtractorUtilities.composeConcatenatedKey(
                    recommendations.get(i).getEntity(), itemKeys);
            int hit = 0;
            if (releItems.contains(recItem)) {
                hit = 1;
                hits += 1;
            }
            for (int j=0; j<N.size(); j++) {
                int n = N.get(j);
                if (rank <= n && hit > 0 && hits > 0) {
                    ap[j] += (1.0 * hits / rank) * hit;
                }
            }
            if (rank > maxN) {
                break;
            }
        }
        for (int i=0; i<N.size(); i++) {
            AP.set(i, AP.getDouble(i) + ap[i] / releItems.size());
        }
        cnt += 1;
    }

    public MetricResult getResults() {
        List<ObjectNode> results = new ArrayList<>(N.size());
        ObjectNode metricPara = Json.newObject();
        metricPara.put("threshold", threshold);
        metricPara.put("minValue", minValue);
        boolean pass = true;
        for (int i=0; i<N.size(); i++) {
            ObjectNode result = Json.newObject();
            result.put(ConfigKey.EVALUATOR_METRIC_NAME.get(), "MAP");
            metricPara.put("N", N.get(i));
            result.put(ConfigKey.EVALUATOR_METRIC_PARA.get(),
                    metricPara.toString());
            double value = 0.0;
            if (cnt > 0) {
                value = AP.getDouble(i) / cnt;
            }
            result.put(ConfigKey.EVALUATOR_METRIC_VALUE.get(), value);
            results.add(result);
            if (value < minValue) {
                pass = false;
            }
        }
        return new MetricResult(results, pass);
    }
}
