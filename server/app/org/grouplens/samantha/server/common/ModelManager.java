package org.grouplens.samantha.server.common;

import org.grouplens.samantha.server.io.RequestContext;

public interface ModelManager {
    Object manage(RequestContext requestContext);
    Object createModel(RequestContext requestContext);
    Object resetModel(RequestContext requestContext);
    Object buildModel(RequestContext requestContext);
    Object buildModel(Object model, RequestContext requestContext);
    Object updateModel(RequestContext requestContext);
    Object updateModel(Object model, RequestContext requestContext);
    Object dumpModel(RequestContext requestContext);
    Object loadModel(RequestContext requestContext);
}
