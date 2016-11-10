package org.grouplens.samantha.server.indexer;

import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public interface IndexerConfig {
    static IndexerConfig getIndexerConfig(Configuration indexerConfig,
                                          Injector injector) {return null;}
    Indexer getIndexer(RequestContext requestContext);
}
