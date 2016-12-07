package org.grouplens.samantha.server.objective;

import org.grouplens.samantha.modeler.ranking.NDCGLoss;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.ranker.RankerUtilities;
import play.Configuration;
import play.inject.Injector;

public class NDCGLossConfig {
    static public ObjectiveFunction getObjectiveFunction(Configuration objectiveConfig,
                                                         Injector injector,
                                                         RequestContext requestContext) {
        double sigma = 1.0;
        int N = RankerUtilities.defaultPageSize;
        if (objectiveConfig.asMap().containsKey("sigma")) {
            sigma = objectiveConfig.getDouble("sigma");
        }
        if (objectiveConfig.asMap().containsKey("N")) {
            N = objectiveConfig.getInt("N");
        }
        return new NDCGLoss(N, sigma);
    }
}
