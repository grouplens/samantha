package org.grouplens.samantha.server.indexer;

import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class JsonFileIndexerConfig implements IndexerConfig {
    private final String filePathKey;

    private JsonFileIndexerConfig(String filePathKey) {
        this.filePathKey = filePathKey;
    }

    public static IndexerConfig getIndexerConfig(Configuration indexerConfig,
                                                 Injector injector) {
        return new JsonFileIndexerConfig(indexerConfig.getString("filePathKey"));
    }

    public Indexer getIndexer(RequestContext requestContext) {
        String filePath = JsonHelpers.getRequiredString(requestContext.getRequestBody(), filePathKey);
        return new JsonFileIndexer(filePath);
    }
}
