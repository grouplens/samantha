package org.grouplens.samantha.server.common;

import org.grouplens.samantha.modeler.space.SpaceMode;
import org.grouplens.samantha.server.io.RequestContext;

public interface ModelManager {
    Object manage(RequestContext requestContext);
    Object createModel(RequestContext requestContext, SpaceMode spaceMode);
    Object resetModel(RequestContext requestContext);
    Object buildModel(RequestContext requestContext);
    Object buildModel(Object model, RequestContext requestContext);
    Object evaluateModel(RequestContext requestContext);
    boolean passModel(Object model, RequestContext requestContext);
    Object updateModel(RequestContext requestContext);
    Object updateModel(Object model, RequestContext requestContext);
    Object dumpModel(RequestContext requestContext);
    Object loadModel(RequestContext requestContext);
}
