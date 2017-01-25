package org.grouplens.samantha.server.solver;

import org.grouplens.samantha.modeler.common.LearningMethod;
import org.grouplens.samantha.modeler.solver.OptimizationMethod;
import org.grouplens.samantha.modeler.solver.ProximalGradientMethod;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class ProximalGradientMethodConfig {
    private ProximalGradientMethodConfig() {}

    public static LearningMethod getLearningMethod(Configuration methodConfig,
                                                   Injector injector,
                                                   RequestContext requestContext) {
        double tol = 5.0;
        if (methodConfig.asMap().containsKey("tol")) {
            tol = methodConfig.getDouble("tol");
        }
        int minIter = 2;
        if (methodConfig.asMap().containsKey("minIter")) {
            minIter = methodConfig.getInt("minIter");
        }
        int maxIter = 50;
        if (methodConfig.asMap().containsKey("maxIter")) {
            maxIter = methodConfig.getInt("maxIter");
        }
        OptimizationMethod onlineMethod = new ProximalGradientMethod(
                maxIter, minIter, tol, methodConfig.getDouble("l1coef"),
                methodConfig.getDouble("l2coef"), methodConfig.getDouble("ro")
        );
        return onlineMethod;
    }
}
