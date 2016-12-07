package org.grouplens.samantha.server.io;

import com.fasterxml.jackson.databind.JsonNode;

import org.grouplens.samantha.server.exception.BadRequestException;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RequestParser {

    @Inject
    private RequestParser() {}

    public RequestContext getJsonRequestContext(String engine, JsonNode requestBody)
            throws BadRequestException {
        RequestContext requestContext = new RequestContext(requestBody, engine);
        return requestContext;
    }
}
