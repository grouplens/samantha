package org.grouplens.samantha.server.evaluator;

import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public interface EvaluatorConfig {
    static EvaluatorConfig getEvaluatorConfig(Configuration evalConfig,
                                              Injector injector) {return null;}
    Evaluator getEvaluator(RequestContext requestContext);
}
