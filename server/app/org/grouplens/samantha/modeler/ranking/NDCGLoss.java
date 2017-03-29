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

package org.grouplens.samantha.modeler.ranking;

import com.google.common.collect.Ordering;
import org.grouplens.samantha.modeler.solver.StochasticOracle;

import java.util.List;

public class NDCGLoss extends AbstractLambdaLoss {
    public NDCGLoss(int N, double sigma) {
        super(N, sigma);
    }

    public double getMetric(int maxN, List<StochasticOracle> topN,
                            double[] scores, double[] relevance) {
        double dcg = 0.0;
        for (int i=0; i<maxN; i++) {
            StochasticOracle oracle = topN.get(i);
            relevance[i] = oracle.getLabel();
            dcg += (Math.pow(2.0, relevance[i]) - 1.0) / Math.log(2 + i);
        }
        Ordering<StochasticOracle> ordering = RankingUtilities.stochasticOracleLabelOrdering();
        List<StochasticOracle> bestTop = ordering.greatestOf(topN, topN.size());
        double maxDcg = 0.0;
        for (int i=0; i<maxN; i++) {
            StochasticOracle oracle = bestTop.get(i);
            maxDcg += (Math.pow(2.0, oracle.getLabel()) - 1.0) / Math.log(2 + i);
        }
        scores[maxN] = maxDcg;
        return dcg / maxDcg;
    }

    public double getDelta(int i, int j, double[] scores, double[] relevance) {
        double dcgi = (Math.pow(2.0, relevance[j]) - 1.0) / Math.log(2 + i);
        double dcgj = (Math.pow(2.0, relevance[i]) - 1.0) / Math.log(2 + j);
        return (dcgi - dcgj) / scores[relevance.length];
    }
}
