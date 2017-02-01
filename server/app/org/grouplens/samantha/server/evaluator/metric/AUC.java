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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import org.grouplens.samantha.modeler.tree.SortingUtilities;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.predictor.Prediction;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

public class AUC implements Metric {
    private final String labelName;
    private final List<double[]> list = new ArrayList<>();
    private final double threshold;
    private int numPos = 0;
    private int N = 0;
    private double sumAUC = 0.0;
    private final AUCType aucType;
    private final double minValue;

    static public double computeAUC(List<double[]> list, double threshold) {
        Ordering<double[]> ordering = SortingUtilities.pairDoubleSecondOrdering();
        ImmutableList<double[]> sorted = ordering.reverse().immutableSortedCopy(list);
        double numPos = 0;
        double numNeg = 0;
        double area = 0;
        for (double[] ele : sorted) {
            if (ele[0] >= threshold) {
                numPos += 1;
            } else {
                numNeg += 1;
                area += numPos;
            }
        }
        return area / (numNeg * numPos);
    }

    private interface aucMethods {
        void add(List<ObjectNode> groundTruth, List<Prediction> predictions, AUC auc);
        double getAUC(AUC auc);
    }

    enum AUCType implements aucMethods {

        GLOBAL("global") {
            public void add(List<ObjectNode> groundTruth, List<Prediction> predictions, AUC auc) {
                auc.N += groundTruth.size();
                addIntoList(groundTruth, predictions, auc);
            }
            public double getAUC(AUC auc) {
                return computeAUC(auc.list, auc.threshold);
            }
        },
        PERGROUP("pergroup") {
            public void add(List<ObjectNode> groundTruth, List<Prediction> predictions, AUC auc) {
                auc.numPos = 0;
                auc.list.clear();
                addIntoList(groundTruth, predictions, auc);
                if (auc.numPos > 0 && auc.numPos < groundTruth.size()) {
                    auc.sumAUC += computeAUC(auc.list, auc.threshold);
                    auc.N++;
                }
            }
            public double getAUC(AUC auc) {
                if (auc.N > 0) {
                    return auc.sumAUC / auc.N;
                } else {
                    return 0.0;
                }
            }
        };
        private final String key;

        AUCType(String key) {
            this.key = key;
        }

        static private void addIntoList(List<ObjectNode> groundTruth, List<Prediction> predictions, AUC auc) {
            for (int i=0; i<groundTruth.size(); i++) {
                double label = groundTruth.get(i).get(auc.labelName).asDouble();
                if (label >= auc.threshold) {
                    auc.numPos++;
                }
                double[] one = new double[2];
                one[0] = label;
                one[1] = predictions.get(i).getScore();
                auc.list.add(one);
            }
        }

        public String get() {
            return this.key;
        }
    }

    public AUC(String labelName, AUCType aucType, double threshold, double minValue) {
        this.labelName = labelName;
        this.aucType = aucType;
        this.threshold = threshold;
        this.minValue = minValue;
    }

    public void add(List<ObjectNode> groundTruth, List<Prediction> predictions) {
        aucType.add(groundTruth, predictions, this);
    }

    public MetricResult getResults() {
        ObjectNode result = Json.newObject();
        result.put(ConfigKey.EVALUATOR_METRIC_NAME.get(), "AUC");
        ObjectNode para = Json.newObject();
        para.put("type", aucType.get());
        para.put("threshold", threshold);
        para.put("N", N);
        result.put(ConfigKey.EVALUATOR_METRIC_PARA.get(), para.toString());
        double value = aucType.getAUC(this);
        result.put(ConfigKey.EVALUATOR_METRIC_VALUE.get(), value);
        List<ObjectNode> results = new ArrayList<>(1);
        results.add(result);
        boolean pass = true;
        if (value < minValue) {
            pass = false;
        }
        return new MetricResult(results, pass);
    }
}
