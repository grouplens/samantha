package org.grouplens.samantha.server.retriever;

import play.Configuration;

public abstract class AbstractRetriever implements Retriever {
    protected final Configuration config;

    public AbstractRetriever(Configuration config) {
        this.config = config;
    }

    public Configuration getConfig() {
        return config;
    }
}
