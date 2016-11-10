package org.grouplens.samantha.server.config;

public enum EngineComponent {
    ROUTER("router"),
    RETRIEVERS("retrievers"),
    PREDICTORS("predictors"),
    RANKERS("rankers"),
    INDEXERS("indexers"),
    EVALUATORS("evaluators"),
    RECOMMENDERS("recommenders");

    private final String component;

    EngineComponent(String component) {
        this.component = component;
    }

    public String get() {
        return component;
    }
}
