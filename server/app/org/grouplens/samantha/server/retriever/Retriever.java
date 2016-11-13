package org.grouplens.samantha.server.retriever;

import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;

public interface Retriever {
    RetrievedResult retrieve(RequestContext requestContext);
    Configuration getConfig();
}
