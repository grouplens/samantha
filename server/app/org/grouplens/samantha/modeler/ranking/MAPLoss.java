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

package org.grouplens.samantha.modeler.ranking;

import org.grouplens.samantha.modeler.solver.StochasticOracle;

import java.util.List;

public class MAPLoss extends AbstractLambdaLoss {
    private final double threshold;

    public MAPLoss(int N, double sigma, double threshold) {
        super(N, sigma);
        this.threshold = threshold;
    }

    public double getMetric(int maxN, List<StochasticOracle> topN,
                            double[] scores, double[] relevance) {
        int numHits = 0;
        double ap = 0.0;
        for (int i=0; i<maxN; i++) {
            StochasticOracle oracle = topN.get(i);
            int rank = i + 1;
            if (oracle.getLabel() >= threshold) {
                relevance[i] = 1.0;
                numHits += 1;
            } else {
                relevance[i] = 0.0;
            }
            scores[i] = 1.0 * numHits / rank;
            if (relevance[i] > 0.5) {
                ap += scores[i];
            }
        }
        scores[maxN] = (double)numHits;
        if (numHits > 0) {
            return ap / numHits;
        } else {
            return 0.0;
        }
    }

    public double getDelta(int i, int j, double[] scores, double[] relevance) {
        double delta = 0.0;
        if (relevance[i] > 0.5) {
            delta -= scores[i];
        } else {
            delta += scores[i] + 1.0 / (i + 1);
        }
        for (int k=i+1; k<j; k++) {
            if (relevance[k] > 0.5) {
                if (relevance[i] > 0.5) {
                    delta -= 1.0 / (k + 1);
                } else {
                    delta += 1.0 / (k + 1);
                }
            }
        }
        if (relevance[j] > 0.5) {
            delta -= scores[j];
        } else {
            delta += scores[j];
        }
        int len = relevance.length;
        if (scores[len] > 0.0) {
            return delta / scores[len];
        } else {
            return 0.0;
        }
    }
}
