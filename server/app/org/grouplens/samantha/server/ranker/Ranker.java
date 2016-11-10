package org.grouplens.samantha.server.ranker;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.retriever.RetrievedResult;

public interface Ranker {
    RankedResult rank(RetrievedResult retrievedResult, RequestContext requestContext);
    JsonNode getFootprint();
}
