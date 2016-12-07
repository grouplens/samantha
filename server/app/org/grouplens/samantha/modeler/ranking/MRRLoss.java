package org.grouplens.samantha.modeler.ranking;

import org.grouplens.samantha.modeler.solver.StochasticOracle;

import java.util.List;

public class MRRLoss extends AbstractLambdaLoss {
    private final double threshold;

    public MRRLoss(int N, double sigma, double threshold) {
        super(N, sigma);
        this.threshold = threshold;
    }

    public double getMetric(int maxN, List<StochasticOracle> topN,
                            double[] scores, double[] relevance) {
        double mrr = 0.0;
        for (int i=0; i<maxN; i++) {
            StochasticOracle oracle = topN.get(i);
            int rank = i + 1;
            if (oracle.getLabel() >= threshold) {
                relevance[i] = 1.0;
                mrr += 1.0 / rank;
            } else {
                relevance[i] = 0.0;
            }
        }
        return mrr;
    }

    public double getDelta(int i, int j, double[] scores, double[] relevance) {
        if (relevance[i] > 0.5) {
            return 1.0 / (j + 1) -  1.0 / (i + 1);
        } else {
            return 1.0 / (i + 1) - 1.0 / (j + 1);
        }
    }
}
