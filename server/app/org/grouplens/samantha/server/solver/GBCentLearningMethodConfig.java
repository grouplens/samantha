package org.grouplens.samantha.server.solver;

import org.grouplens.samantha.modeler.boosting.GBCentLearningMethod;
import org.grouplens.samantha.modeler.common.LearningMethod;
import org.grouplens.samantha.modeler.solver.OnlineOptimizationMethod;
import org.grouplens.samantha.modeler.tree.TreeLearningMethod;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.PredictorUtilities;
import play.Configuration;
import play.inject.Injector;

public class GBCentLearningMethodConfig implements LearningMethodConfig {
    private GBCentLearningMethodConfig() {}

    public static LearningMethod getLearningMethod(Configuration methodConfig,
                                                   Injector injector,
                                                   RequestContext requestContext) {
        double minTreeGain = 0.0;
        if (methodConfig.asMap().containsKey("minTreeGain")) {
            minTreeGain = methodConfig.getDouble("minTreeGain");
        }
        GBCentLearningMethod method = new GBCentLearningMethod(
                (OnlineOptimizationMethod) PredictorUtilities.getLearningMethod(methodConfig
                        .getConfig("onlineOptimizationMethod"), injector, requestContext),
                (TreeLearningMethod) PredictorUtilities.getLearningMethod(methodConfig
                        .getConfig("treeLearningMethod"), injector, requestContext),
                methodConfig.getInt("minSupport"),
                methodConfig.getBoolean("learnSvdfea"),
                minTreeGain
        );
        return method;
    }
}
