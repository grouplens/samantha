package org.grouplens.samantha.server.ranker;

import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import play.Configuration;

public interface Ranker {
    RankedResult rank(RetrievedResult retrievedResult, RequestContext requestContext);
    Configuration getConfig();
}
