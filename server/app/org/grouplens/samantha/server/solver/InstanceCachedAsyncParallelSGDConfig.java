package org.grouplens.samantha.server.solver;

import org.grouplens.samantha.modeler.common.LearningMethod;
import org.grouplens.samantha.modeler.solver.InstanceCachedAsyncParallelSGD;
import org.grouplens.samantha.modeler.solver.OptimizationMethod;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class InstanceCachedAsyncParallelSGDConfig {

    private InstanceCachedAsyncParallelSGDConfig() {}

    public static LearningMethod getLearningMethod(Configuration methodConfig,
                                                   Injector injector,
                                                   RequestContext requestContext) {
        double tol = 5.0;
        if (methodConfig.asMap().containsKey("tol")) {
            tol = methodConfig.getDouble("tol");
        }
        int num = Runtime.getRuntime().availableProcessors();
        if (methodConfig.asMap().containsKey("numProcessors")) {
            num = methodConfig.getInt("numProcessors");
        }
        OptimizationMethod optMethod = new InstanceCachedAsyncParallelSGD(
                methodConfig.getInt("maxIter"),
                methodConfig.getDouble("l2coef"),
                methodConfig.getDouble("learningRate"),
                tol, num,
                methodConfig.getString("cachePath")
        );
        return optMethod;
    }
}
