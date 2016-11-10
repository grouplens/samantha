package org.grouplens.samantha.server.common;

public enum ModelOperation {
    BUILD("build"),
    UPDATE("update"),
    DUMP("dump"),
    LOAD("load"),
    RESET("reset");

    final private String key;

    ModelOperation(String key) {
        this.key = key;
    }

    public String get() {
        return key;
    }
}
