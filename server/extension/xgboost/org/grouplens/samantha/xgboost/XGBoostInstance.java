package org.grouplens.samantha.xgboost;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import ml.dmlc.xgboost4j.LabeledPoint;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.featurizer.StandardLearningInstance;

public class XGBoostInstance implements LearningInstance {

    private final StandardLearningInstance instance;

    public XGBoostInstance(StandardLearningInstance instance) {
        this.instance = instance;
    }

    public LabeledPoint getLabeledPoint() {
        Int2DoubleMap features = instance.getFeatures();
        float[] values = new float[features.size()];
        double[] froms = features.values().toDoubleArray();
        for (int i=0; i<froms.length; i++) {
            values[i] = (float) froms[i];
        }
        return LabeledPoint.fromSparseVector((float) instance.getLabel(),
                features.keySet().toIntArray(), values);
    }

    public double getLabel() {
        return instance.getLabel();
    }

    public LearningInstance newInstanceWithLabel(double label) {
        return new XGBoostInstance((StandardLearningInstance)instance.newInstanceWithLabel(label));
    }

    public double getWeight() {
        return instance.getWeight();
    }

    public void setLabel(double label) {
        this.instance.setLabel(label);
    }

    public void setWeight(double weight) {
        this.instance.setWeight(weight);
    }
}
