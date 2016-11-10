package org.grouplens.samantha.modeler.tree;

public enum TreeKey {
    TREE("TREE");

    private final String key;

    TreeKey(String key) {
        this.key = key;
    }

    public String get() {
        return key;
    }
}
