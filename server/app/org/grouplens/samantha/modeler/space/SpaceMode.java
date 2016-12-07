package org.grouplens.samantha.modeler.space;

public enum SpaceMode {
    BUILDING("building"),
    DEFAULT("default");

    private final String key;

    SpaceMode(String key) {
        this.key = key;
    }

    public String get() {
        return key;
    }
}
