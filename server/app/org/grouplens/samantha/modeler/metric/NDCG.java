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
import com.google.common.collect.Ordering;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.retriever.RetrieverUtilities;
import play.Logger;

import java.util.List;

public class NDCG implements Metric {
    private final List<Integer> N;
    private final List<String> itemKeys;
    private final List<String> recKeys;
    private final String relevanceKey;
    private final double minValue;
    private int cnt = 0;
    private DoubleList DCG;

    public NDCG(List<Integer> N, List<String> itemKeys, List<String> recKeys,
                String relevanceKey, double minValue) {
        this.N = N;
        this.itemKeys = itemKeys;
        this.recKeys = recKeys;
        this.relevanceKey = relevanceKey;
        this.minValue = minValue;
        this.DCG = new DoubleArrayList(N.size());
        for (int i=0; i<N.size(); i++) {
            this.DCG.add(0.0);
        }
    }

    public void add(List<ObjectNode> groundTruth, List<Prediction> recommendations) {
        Object2DoubleMap<String> releItems = new Object2DoubleOpenHashMap<>();
        for (JsonNode entity : groundTruth) {
            String item = FeatureExtractorUtilities.composeConcatenatedKeyWithoutName(entity, itemKeys);
            releItems.put(item, entity.get(relevanceKey).asDouble());
        }
        int maxN = 0;
        for (Integer n : N) {
            if (n > maxN) {
                maxN = n;
            }
            if (recommendations.size() < n) {
                Logger.error("The number of recommendations({}) is less than the indicated NDCG N({})",
                        recommendations.size(), n);
            }
        }
        double[] dcg = new double[N.size()];
        for (int i=0; i<recommendations.size(); i++) {
            int rank = i + 1;
            String recItem = FeatureExtractorUtilities.composeConcatenatedKeyWithoutName(
                    recommendations.get(i).getEntity(), recKeys);
            if (releItems.containsKey(recItem)) {
                for (int j=0; j<N.size(); j++) {
                    int n = N.get(j);
                    if (rank <= n) {
                        dcg[j] += (Math.pow(2.0, releItems.getDouble(recItem)) / Math.log(1.0 + rank));
                    }
                }
            }
            if (rank > maxN) {
                break;
            }
        }
        double[] maxDcg = new double[N.size()];
        if (groundTruth.size() <= maxN) {
            maxN = groundTruth.size();
        }
        Ordering<ObjectNode> ordering = RetrieverUtilities.jsonFieldOrdering(relevanceKey);
        List<ObjectNode> topN = ordering.greatestOf(groundTruth, maxN);
        for (int i=0; i<topN.size(); i++) {
            int rank = i + 1;
            double relevance = topN.get(i).get(relevanceKey).asDouble();
            if (relevance > 0.0) {
                for (int j=0; j<N.size(); j++) {
                    int n = N.get(j);
                    if (rank <= n) {
                        maxDcg[j] += (Math.pow(2.0, relevance) / Math.log(1.0 + rank));
                    }
                }
            } else {
                break;
            }
        }
        for (int i=0; i<N.size(); i++) {
            DCG.set(i, DCG.getDouble(i) + dcg[i] / maxDcg[i]);
        }
        cnt += 1;
    }

    public MetricResult getResults() {
        return MetricUtilities.getTopNResults("nDCG", N, null, minValue, DCG, cnt);
    }
}
