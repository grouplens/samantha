package org.grouplens.samantha.server.indexer;

import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class StandardOutputIndexerConfig implements IndexerConfig {

    private StandardOutputIndexerConfig() {}

    public static IndexerConfig getIndexerConfig(Configuration indexerConfig,
                                                 Injector injector) {
        return new StandardOutputIndexerConfig();
    }

    public Indexer getIndexer(RequestContext requestContext) {
        return new StandardOutputIndexer();
    }
}
