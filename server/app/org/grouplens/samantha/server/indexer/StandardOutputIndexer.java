package org.grouplens.samantha.server.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.server.io.RequestContext;
import play.Logger;

public class StandardOutputIndexer implements Indexer {

    public void index(String type, JsonNode documents, RequestContext requestContext) {
        Logger.info(documents.toString());
    }

    public void index(RequestContext requestContext) {
        Logger.info(requestContext.getRequestBody().toString());
    }
}
