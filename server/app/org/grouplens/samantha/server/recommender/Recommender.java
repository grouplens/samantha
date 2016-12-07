package org.grouplens.samantha.server.recommender;

import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.ranker.RankedResult;
import play.Configuration;

public interface Recommender {
    RankedResult recommend(RequestContext requestContext) throws BadRequestException;
    Configuration getConfig();
}
