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
            loglik += (label * modelOutput - Math.log(1.0 + Math.exp(modelOutput)));
        }
        return loglik;
    }

    public double getDelta(int i, int j, double[] scores, double[] relevance) {
        double dcgi = (Math.pow(2.0, relevance[j]) - 1.0) / Math.log(2 + i);
        double dcgj = (Math.pow(2.0, relevance[i]) - 1.0) / Math.log(2 + j);
        return dcgi - dcgj;
    }
}
