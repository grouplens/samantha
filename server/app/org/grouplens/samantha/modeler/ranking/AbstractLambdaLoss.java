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

import com.google.common.collect.Ordering;
import org.grouplens.samantha.modeler.solver.StochasticOracle;

import java.util.ArrayList;
import java.util.List;

abstract public class AbstractLambdaLoss implements LambdaLoss {
    private final int N;
    private final double sigma;

    /**
     * @param N when N is zero, all observations are used.
     */
    public AbstractLambdaLoss(int N, double sigma) {
        this.N = N;
        this.sigma = sigma;
    }

    public List<StochasticOracle> wrapOracle(List<StochasticOracle> oracles) {
        if (oracles.size() <= 1) {
            return new ArrayList<>();
        }
        int maxN = N;
        if (maxN == 0 || maxN > oracles.size()) {
            maxN = oracles.size();
        }
        List<StochasticOracle> newOracles = new ArrayList<>(maxN);
        Ordering<StochasticOracle> ordering = RankingUtilities.stochasticOracleOrdering();
        List<StochasticOracle> topN = ordering.greatestOf(oracles, maxN);
        double[] scores = new double[maxN + 1];
        double[] relevance = new double[maxN];
        double metric = getMetric(maxN, topN, scores, relevance);
        double[] lambdas = new double[maxN];
        for (int i=0; i<maxN; i++) {
            for (int j=i+1; j<maxN; j++) {
                if (relevance[i] != relevance[j]) {
                    StochasticOracle highOracle = topN.get(i);
                    StochasticOracle lowOracle = topN.get(j);
                    double diff = (highOracle.getModelOutput() - lowOracle.getModelOutput());
                    double ijCoef = -sigma / (1.0 + Math.exp(sigma * diff));
                    double jiCoef = -sigma / (1.0 + Math.exp(-sigma * diff));
                    double delta = Math.abs(getDelta(i, j, scores, relevance));
                    if (relevance[i] > relevance[j]) {
                        lambdas[i] += ijCoef * delta;
                        lambdas[j] -= jiCoef * delta;
                    } else {
                        lambdas[i] -= ijCoef * delta;
                        lambdas[j] += jiCoef * delta;
                    }
                }
            }
        }
        double objVal = - metric / maxN;
        for (int i=0; i<maxN; i++) {
            StochasticOracle oracle = topN.get(i);
            double weight = oracle.getWeight();
            oracle.setObjVal(objVal * weight);
            oracle.setGradient(lambdas[i] * weight);
            newOracles.add(oracle);
        }
        return newOracles;
    }
}
