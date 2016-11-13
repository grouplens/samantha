package org.grouplens.samantha.server.predictor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;

import java.util.List;

public interface Predictor {
    /**
     * The size of List<Prediction> must be consistent with the size List<ObjectNode> entityList.
     */
    List<Prediction> predict(List<ObjectNode> entityList, RequestContext requestContext);
    List<Prediction> predict(RequestContext requestContext);
    Configuration getConfig();
}
