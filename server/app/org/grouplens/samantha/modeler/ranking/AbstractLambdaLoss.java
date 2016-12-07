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
        if (oracles.size() == 0) {
            return oracles;
        }
        oracles.sort(RankingUtilities.stochasticOracleReverseComparator());
        int maxN = N;
        List<StochasticOracle> newOracles;
        if (maxN == 0 || maxN > oracles.size()) {
            maxN = oracles.size();
            newOracles = oracles;
        } else {
            newOracles = new ArrayList<>(maxN);
        }
        Ordering<StochasticOracle> ordering = RankingUtilities.stochasticOracleOrdering();
        List<StochasticOracle> topN = ordering.greatestOf(oracles, maxN);
        double[] scores = new double[maxN];
        double[] relevance = new double[maxN];
        double metric = getMetric(maxN, topN, scores, relevance);
        double[] lambdas = new double[maxN];
        for (int i=0; i<maxN; i++) {
            for (int j=i+1; j<maxN; j++) {
                if (relevance[i] != relevance[j]) {
                    StochasticOracle highOracle = topN.get(i);
                    StochasticOracle lowOracle = topN.get(j);
                    double deltaAP = getDelta(i, j, scores, relevance);
                    double diff = (highOracle.getModelOutput() - lowOracle.getModelOutput());
                    double ijCoef = -sigma / (1.0 + Math.exp(sigma * diff));
                    double jiCoef = -sigma / (1.0 + Math.exp(-sigma * diff));
                    lambdas[i] += ijCoef * deltaAP;
                    lambdas[j] -= jiCoef * deltaAP;
                }
            }
        }
        double objVal = - metric / maxN;
        for (int i=0; i<maxN; i++) {
            StochasticOracle oracle = topN.get(i);
            double weight = oracle.getWeight();
            oracle.setObjVal(objVal * weight);
            oracle.setGradient(lambdas[i] * weight);
            if (newOracles.size() <= i) {
                newOracles.add(oracle);
            }
        }
        return newOracles;
    }
}
