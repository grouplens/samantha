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
