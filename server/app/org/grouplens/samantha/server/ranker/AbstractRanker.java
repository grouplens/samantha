package org.grouplens.samantha.server.ranker;

import play.Configuration;

public abstract class AbstractRanker implements Ranker {
    protected final Configuration config;

    public AbstractRanker(Configuration config) {
        this.config = config;
    }

    public Configuration getConfig() {
        return config;
    }
}
