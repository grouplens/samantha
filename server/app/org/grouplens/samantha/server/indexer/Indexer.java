package org.grouplens.samantha.server.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.server.io.RequestContext;

public interface Indexer {
    void index(RequestContext requestContext);
    void index(String type, JsonNode data, RequestContext requestContext);
}
