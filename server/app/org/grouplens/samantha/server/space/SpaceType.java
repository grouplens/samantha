package org.grouplens.samantha.server.space;

public enum SpaceType {
    INDEX("index"),
    VARIABLE("variable");

    private final String key;

    SpaceType(String key) {
        this.key = key;
    }

    public String get() {
        return key;
    }
}
