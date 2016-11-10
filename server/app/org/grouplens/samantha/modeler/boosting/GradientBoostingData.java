package org.grouplens.samantha.modeler.boosting;

import it.unimi.dsi.fastutil.doubles.DoubleList;

import it.unimi.dsi.fastutil.ints.IntList;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;

public class GradientBoostingData implements LearningData {
    final private LearningData learningData;
    final private DoubleList preds;
    final private IntList subset;
    final private ObjectiveFunction objectiveFunction;
    private int idx = 0;

    public GradientBoostingData(LearningData learningData, DoubleList preds,
                                IntList subset, ObjectiveFunction objectiveFunction) {
        this.learningData = learningData;
        this.preds = preds;
        this.objectiveFunction = objectiveFunction;
        this.subset = subset;
    }

    public LearningInstance getLearningInstance() {
        LearningInstance ins = learningData.getLearningInstance();
        if (ins == null) {
            return null;
        }
        double modelOutput = 0.0;
        if (preds != null) {
            int subidx = idx;
            if (subset != null) {
                subidx = subset.getInt(idx);
            }
            modelOutput += preds.getDouble(subidx);
            idx++;
        }
        return ins.newInstanceWithLabel(-objectiveFunction.getGradient(modelOutput,
                ins.getLabel(), ins.getWeight()));
    }

    public void startNewIteration() {
        learningData.startNewIteration();
        idx = 0;
    }
}
