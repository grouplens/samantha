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

public class PairwiseLoss extends AbstractLambdaLoss {
    private final double threshold;

    public PairwiseLoss(int N, double sigma, double threshold) {
        super(N, sigma);
        this.threshold = threshold;
    }

    public double getMetric(int maxN, List<StochasticOracle> topN,
                            double[] scores, double[] relevance) {
        double loglik = 0.0;
        for (int i=0; i<maxN; i++) {
            StochasticOracle oracle = topN.get(i);
            double label = 1.0;
            if (oracle.getLabel() >= threshold) {
                relevance[i] = 1.0;
            } else {
                relevance[i] = 0.0;
            }
            double modelOutput = oracle.getModelOutput();
            scores[i] = modelOutput;
            loglik += (label * modelOutput - Math.log(1.0 + Math.exp(modelOutput)));
        }
        return loglik;
    }

    public double getDelta(int i, int j, double[] scores, double[] relevance) {
        double logliki = (relevance[i] * scores[i] - Math.log(1.0 + Math.exp(scores[i])));
        double loglikj = (relevance[j] * scores[j] - Math.log(1.0 + Math.exp(scores[j])));
        return logliki - loglikj;
    }
}
