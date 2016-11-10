package org.grouplens.samantha.server.io;

import com.fasterxml.jackson.databind.JsonNode;

import javax.inject.Inject;

public class RequestContext {

    final private String engineName;
    final private JsonNode requestBody;

    @Inject
    public RequestContext(JsonNode requestBody,
                          String engineName) {
        this.requestBody = requestBody;
        this.engineName = engineName;
    }

    public String getEngineName() {
        return engineName;
    }

    public JsonNode getRequestBody() {
        return requestBody;
    }

}
