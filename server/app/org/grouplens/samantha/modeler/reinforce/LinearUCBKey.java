package org.grouplens.samantha.modeler.reinforce;

public enum LinearUCBKey {
    BIASES("BIASES"),
    A("A"),
    B("B");

    private final String key;

    LinearUCBKey(String key) {
        this.key = key;
    }

    public String get() {
        return key;
    }
}
