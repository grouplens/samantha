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
        return ap / numHits;
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
        return delta;
    }
}
