package org.grouplens.samantha.server.objective;

import org.grouplens.samantha.modeler.ranking.MRRLoss;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.ranker.RankerUtilities;
import play.Configuration;
import play.inject.Injector;

public class MRRLossConfig {
    static public ObjectiveFunction getObjectiveFunction(Configuration objectiveConfig,
                                                         Injector injector,
                                                         RequestContext requestContext) {
        double sigma = 1.0;
        int N = RankerUtilities.defaultPageSize;
        double threshold = 0.5;
        if (objectiveConfig.asMap().containsKey("sigma")) {
            sigma = objectiveConfig.getDouble("sigma");
        }
        if (objectiveConfig.asMap().containsKey("N")) {
            N = objectiveConfig.getInt("N");
        }
        if (objectiveConfig.asMap().containsKey("threshold")) {
            threshold = objectiveConfig.getDouble("threshold");
        }
        return new MRRLoss(N, sigma, threshold);
    }
}
