package org.grouplens.samantha.server.solver;

import org.grouplens.samantha.modeler.common.LearningMethod;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public interface LearningMethodConfig {
    static LearningMethod getLearningMethod(Configuration expanderConfig,
                                            Injector injector,
                                            RequestContext requestContext) {return null;}
}
