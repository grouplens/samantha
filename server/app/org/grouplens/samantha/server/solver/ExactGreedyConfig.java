package org.grouplens.samantha.server.solver;

import org.grouplens.samantha.modeler.common.LearningMethod;
import org.grouplens.samantha.modeler.tree.ExactGreedy;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class ExactGreedyConfig implements LearningMethodConfig {
    private ExactGreedyConfig() {}

    public static LearningMethod getLearningMethod(Configuration methodConfig,
                                                   Injector injector,
                                                   RequestContext requestContext) {
        int minNodeSplit = 50;
        if (methodConfig.asMap().containsKey("minNodeSplit")) {
            minNodeSplit = methodConfig.getInt("minNodeSplit");
        }
        int maxTreeDepth = methodConfig.getInt("maxTreeDepth");
        return new ExactGreedy(minNodeSplit, maxTreeDepth);
    }
}
