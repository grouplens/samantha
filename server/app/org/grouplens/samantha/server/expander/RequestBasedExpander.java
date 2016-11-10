package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class RequestBasedExpander implements EntityExpander {
    final private List<String> requestFields;

    public RequestBasedExpander(List<String> requestFields) {
        this.requestFields = requestFields;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        return new RequestBasedExpander(expanderConfig.getStringList("requestFields"));
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult, RequestContext requestContext) {
        JsonNode requestBody = requestContext.getRequestBody();
        List<ObjectNode> expandedResult = new ArrayList<>();
        for (ObjectNode entity : initialResult) {
            IOUtilities.parseEntityFromJsonNode(requestFields, requestBody, entity);
            expandedResult.add(entity);
        }
        return expandedResult;
    }
}
