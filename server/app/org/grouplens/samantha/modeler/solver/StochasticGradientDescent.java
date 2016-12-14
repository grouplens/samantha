package org.grouplens.samantha.modeler.solver;

import org.grouplens.samantha.modeler.common.LearningData;

public class StochasticGradientDescent extends AbstractOptimizationMethod implements OnlineOptimizationMethod {
    private double l2coef;
    private double lr;

    public StochasticGradientDescent() {
        super(5.0, 50);
        l2coef = 0.0;
        lr = 0.001;
    }

    public StochasticGradientDescent(int maxIter, double l2coef, double learningRate, double tol) {
        super(tol, maxIter);
        this.maxIter = maxIter;
        this.l2coef = l2coef;
        this.lr = learningRate;
        this.tol = tol;
    }

    public double update(LearningModel model, LearningData learningData) {
        L2Regularizer l2term = new L2Regularizer();
        ObjectiveFunction objFunc = model.getObjectiveFunction();
        learningData.startNewIteration();
        double objVal = SolverUtilities.stochasticGradientDescentUpdate(model, objFunc,
                learningData, l2term, l2coef, lr);
        return objVal;
    }
}
