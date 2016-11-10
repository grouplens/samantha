package org.grouplens.samantha.modeler.solver;

import org.grouplens.samantha.modeler.common.LearningData;

public class SGDRunnable implements ObjectiveRunnable {
    private final LearningModel learningModel;
    private final LearningData learningData;
    private final double l2coef;
    private final double lr;
    private double objVal = 0.0;

    SGDRunnable(LearningModel learningModel, LearningData learningData, double l2coef, double lr) {
        this.learningData = learningData;
        this.learningModel = learningModel;
        this.l2coef = l2coef;
        this.lr = lr;
    }

    public void run() {
        L2Regularizer l2term = new L2Regularizer();
        ObjectiveFunction objFunc = learningModel.getObjectiveFunction();
        objVal += SolverUtilities.stochasticGradientDescentUpdate(learningModel, objFunc,
                learningData, l2term, l2coef, lr);
    }

    public double getObjVal() {
        return objVal;
    }
}
