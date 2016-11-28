package org.grouplens.samantha.server.common;

import org.grouplens.samantha.server.io.RequestContext;

interface ModelOperator {
    void operate(ModelManager modelManager, RequestContext requestContext);
}
