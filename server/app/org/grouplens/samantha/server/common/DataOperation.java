package org.grouplens.samantha.server.common;

public enum DataOperation {
    UPSERT("UPSERT"),
    INSERT("INSERT"),
    DELETE("DELETE"),
    ;
    private final String key;

    DataOperation(String key) {
        this.key = key;
    }

    public String get() {
        return key;
    }
}
