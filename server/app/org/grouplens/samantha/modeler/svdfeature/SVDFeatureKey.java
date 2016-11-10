package org.grouplens.samantha.modeler.svdfeature;

public enum SVDFeatureKey {
    BIASES("BIASES"),
    FACTORS("FACTORS"),
    SUPPORT("SUPPORT");

    private final String key;

    SVDFeatureKey(String key) {
        this.key = key;
    }

    public String get() {
        return key;
    }
}
