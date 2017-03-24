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

package org.grouplens.samantha.server.evaluator.metric;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.retriever.RetrieverUtilities;
import play.Logger;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

public class NDCG implements Metric {
    private final NDCGConfig config;
    private int cnt = 0;
    private DoubleList DCG;

    public NDCG(NDCGConfig config) {
        this.config = config;
        this.DCG = new DoubleArrayList(config.N.size());
        for (int i=0; i<config.N.size(); i++) {
            this.DCG.add(0.0);
        }
    }

    public void add(List<ObjectNode> groundTruth, List<Prediction> recommendations) {
        Object2DoubleMap<String> releItems = new Object2DoubleOpenHashMap<>();
        for (JsonNode entity : groundTruth) {
            String item = FeatureExtractorUtilities.composeConcatenatedKey(entity, config.itemKeys);
            releItems.put(item, entity.get(config.relevanceKey).asDouble());
        }
        int maxN = 0;
        for (Integer n : config.N) {
            if (n > maxN) {
                maxN = n;
            }
            if (recommendations.size() < n) {
                Logger.error("The number of recommendations({}) is less than the indicated NDCG N({})",
                        recommendations.size(), n);
            }
        }
        double[] dcg = new double[config.N.size()];
        for (int i=0; i<recommendations.size(); i++) {
            int rank = i + 1;
            String recItem = FeatureExtractorUtilities.composeConcatenatedKey(
                    recommendations.get(i).getEntity(), config.itemKeys);
            if (releItems.containsKey(recItem)) {
                for (int j=0; j<config.N.size(); j++) {
                    int n = config.N.get(j);
                    if (rank <= n) {
                        dcg[j] += (Math.pow(2.0, releItems.getDouble(recItem)) / Math.log(1.0 + rank));
                    }
                }
            }
            if (rank > maxN) {
                break;
            }
        }
        double[] maxDcg = new double[config.N.size()];
        if (groundTruth.size() <= maxN) {
            maxN = groundTruth.size();
        }
        Ordering<ObjectNode> ordering = RetrieverUtilities.jsonFieldOrdering(config.relevanceKey);
        List<ObjectNode> topN = ordering.greatestOf(groundTruth, maxN);
        for (int i=0; i<topN.size(); i++) {
            int rank = i + 1;
            double relevance = topN.get(i).get(config.relevanceKey).asDouble();
            if (relevance > 0.0) {
                for (int j=0; j<config.N.size(); j++) {
                    int n = config.N.get(j);
                    if (rank <= n) {
                        maxDcg[j] += (Math.pow(2.0, relevance) / Math.log(1.0 + rank));
                    }
                }
            } else {
                break;
            }
        }
        for (int i=0; i<config.N.size(); i++) {
            DCG.set(i, DCG.getDouble(i) + dcg[i] / maxDcg[i]);
        }
        cnt += 1;
    }

    public MetricResult getResults() {
        List<ObjectNode> results = new ArrayList<>(config.N.size());
        ObjectNode metricPara = Json.newObject();
        metricPara.put("minValue", config.minValue);
        boolean pass = true;
        for (int i=0; i<config.N.size(); i++) {
            ObjectNode result = Json.newObject();
            result.put(ConfigKey.EVALUATOR_METRIC_NAME.get(), "NDCG");
            metricPara.put("N", config.N.get(i));
            result.put(ConfigKey.EVALUATOR_METRIC_PARA.get(),
                    metricPara.toString());
            double value = 0.0;
            if (cnt > 0) {
                value = DCG.getDouble(i) / cnt;
            }
            result.put(ConfigKey.EVALUATOR_METRIC_VALUE.get(), value);
            results.add(result);
            if (value < config.minValue) {
                pass = false;
            }
        }
        return new MetricResult(results, pass);
    }
}
