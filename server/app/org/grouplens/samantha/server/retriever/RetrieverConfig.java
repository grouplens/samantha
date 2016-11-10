package org.grouplens.samantha.server.retriever;

import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public interface RetrieverConfig {
    static RetrieverConfig getRetrieverConfig(Configuration retrieverConfig,
                                              Injector injector) {return null;}
    Retriever getRetriever(RequestContext requestContext);
}
