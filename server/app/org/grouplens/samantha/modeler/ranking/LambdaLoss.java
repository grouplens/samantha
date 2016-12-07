package org.grouplens.samantha.modeler.ranking;

import org.grouplens.samantha.modeler.solver.StochasticOracle;

import java.util.List;

public interface LambdaLoss extends RankingLoss {
    double getDelta(int i, int j, double[] scores, double[] relevance);
    double getMetric(int maxN, List<StochasticOracle> topN, double[] scores, double[] relevances);
}
