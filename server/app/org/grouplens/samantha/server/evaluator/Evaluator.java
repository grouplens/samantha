package org.grouplens.samantha.server.evaluator;

import org.grouplens.samantha.server.io.RequestContext;

public interface Evaluator {
    Evaluation evaluate(RequestContext requestContext);
}
