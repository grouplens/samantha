package org.grouplens.samantha.modeler.reinforce;

//TODO: variable doesn't need to be public to users, so move A and B into the class as static members. similar to svdfeature
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
