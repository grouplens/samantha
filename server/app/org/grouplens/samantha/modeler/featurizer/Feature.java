package org.grouplens.samantha.modeler.featurizer;

import java.io.Serializable;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class Feature implements Serializable {
    private double value;
    private int index;

    public Feature() {
        value = 0;
        index = 0;
    }

    public Feature(int index, double value) {
        this.value = value;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
