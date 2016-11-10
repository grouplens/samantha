package org.grouplens.samantha.server.solver;

import org.grouplens.samantha.modeler.common.LearningMethod;
import org.grouplens.samantha.modeler.solver.OnlineOptimizationMethod;
import org.grouplens.samantha.modeler.solver.StochasticGradientDescent;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class StochasticGradientDescentConfig implements LearningMethodConfig {

    private StochasticGradientDescentConfig() {}

    public static LearningMethod getLearningMethod(Configuration methodConfig,
                                                   Injector injector,
                                                   RequestContext requestContext) {
        double tol = 5.0;
        if (methodConfig.asMap().containsKey("tol")) {
            tol = methodConfig.getDouble("tol");
        }
        OnlineOptimizationMethod onlineMethod = new StochasticGradientDescent(
                methodConfig.getInt("maxIter"), methodConfig.getDouble("l2coef"),
                methodConfig.getDouble("learningRate"), tol
        );
        return onlineMethod;
    }
}
