package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.server.exception.InvalidRequestException;

import org.grouplens.samantha.server.io.RequestContext;

public interface Retriever {
    RetrievedResult retrieve(RequestContext requestContext);
    JsonNode getFootprint();
}
