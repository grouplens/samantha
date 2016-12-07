package org.grouplens.samantha.server.objective;

import org.grouplens.samantha.modeler.solver.L2NormLoss;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class L2NormLossConfig {
    static public ObjectiveFunction getObjectiveFunction(Configuration objectiveConfig,
                                                         Injector injector,
                                                         RequestContext requestContext) {
        return new L2NormLoss();
    }
}
