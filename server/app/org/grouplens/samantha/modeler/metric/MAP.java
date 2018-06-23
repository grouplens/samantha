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

package org.grouplens.samantha.modeler.metric;

import com.fasterxml.jackson.databind.node.ObjectNode;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.server.predictor.Prediction;
import play.Logger;

import java.util.List;
import java.util.Set;

public class MAP implements Metric {
    private int cnt = 0;
    private DoubleList AP;
    private final List<Integer> N;
    private final List<String> itemKeys;
    private final List<String> recKeys;
    private final String relevanceKey;
    private final String separator;
    private final double threshold;
    private final double minValue;

    public MAP(List<Integer> N, List<String> itemKeys, List<String> recKeys, String relevanceKey,
               String separator, double threshold, double minValue) {
        this.N = N;
        this.itemKeys = itemKeys;
        this.recKeys = recKeys;
        this.relevanceKey = relevanceKey;
        this.separator = separator;
        this.threshold = threshold;
        this.minValue = minValue;
        this.AP = new DoubleArrayList(N.size());
        for (int i=0; i<N.size(); i++) {
            this.AP.add(0.0);
        }
    }

    public void add(List<ObjectNode> groundTruth, List<Prediction> recommendations) {
        Set<String> releItems = MetricUtilities.getRelevantItems(
                itemKeys, separator, relevanceKey, threshold, groundTruth);
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
            String recItem = FeatureExtractorUtilities.composeConcatenatedKeyWithoutName(
                    recommendations.get(i).getEntity(), recKeys);
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
            AP.set(i, AP.getDouble(i) +
                    ap[i] / Math.min(releItems.size(), N.get(i)));
        }
        cnt += 1;
    }

    public MetricResult getResults() {
        return MetricUtilities.getTopNResults("MAP", N, threshold, minValue, AP, cnt);
    }
}
