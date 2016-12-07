package org.grouplens.samantha.modeler.featurizer;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import org.grouplens.samantha.modeler.common.LearningInstance;

public class StandardLearningInstance extends AbstractLearningInstance {
    private static final long serialVersionUID = 1L;
    public static double defaultWeight = 1.0;
    public static double defaultLabel = 0.0;
    double weight;
    double label;
    final Int2DoubleMap features;

    public StandardLearningInstance(Int2DoubleMap features, double label, double weight, String group) {
        super(group);
        this.features = features;
        this.weight = weight;
        this.label = label;
    }

    public LearningInstance newInstanceWithLabel(double label) {
        return new StandardLearningInstance(this.features, label, this.weight, this.group);
    }

    public Int2DoubleMap getFeatures() {
        return features;
    }

    public double getLabel() {
        return this.label;
    }

    public double getWeight() {
        return this.weight;
    }

    public void setLabel(double label) {
        this.label = label;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
