package org.grouplens.samantha.server.evaluator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.io.RequestContext;

import java.util.List;

public interface Evaluator {
    List<ObjectNode> evaluate(RequestContext requestContext);
}
