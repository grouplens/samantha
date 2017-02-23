package org.grouplens.samantha.ephemeral;

/**
 * Represents a value and its associated weight.
 * Useful when trying to take a weighted average of values.
 */
public class WeightedDouble {
    double value;
    double weight;

    public WeightedDouble(double value, double weight) {
        this.value = value;
        this.weight = weight;
    }
}
