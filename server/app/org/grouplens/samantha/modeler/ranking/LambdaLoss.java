package org.grouplens.samantha.modeler.ranking;

import org.grouplens.samantha.modeler.solver.StochasticOracle;

import java.util.List;

public interface LambdaLoss extends RankingLoss {
    /**
     * @param scores the length should be the length of relevance + 1 where the last element is used for storing useful information.
     */
    double getDelta(int i, int j, double[] scores, double[] relevance);
    double getMetric(int maxN, List<StochasticOracle> topN, double[] scores, double[] relevance);
}
