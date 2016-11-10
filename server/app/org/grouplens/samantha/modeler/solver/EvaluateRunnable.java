package org.grouplens.samantha.modeler.solver;

import org.grouplens.samantha.modeler.common.LearningData;

public class EvaluateRunnable implements ObjectiveRunnable {
    private final LearningModel learningModel;
    private final LearningData learningData;
    private double objVal = 0.0;

    EvaluateRunnable(LearningModel learningModel, LearningData learningData) {
        this.learningData = learningData;
        this.learningModel = learningModel;
    }

    public void run() {
        objVal = SolverUtilities.evaluate(learningModel, learningData);
    }

    public double getObjVal() {
        return objVal;
    }
}
