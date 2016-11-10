package org.grouplens.samantha.server.recommender;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.server.exception.InvalidRequestException;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.ranker.RankedResult;

public interface Recommender {
    RankedResult recommend(RequestContext requestContext) throws InvalidRequestException;
    JsonNode getFootprint();
}
