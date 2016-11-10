package org.grouplens.samantha.modeler.solver;

import org.grouplens.samantha.modeler.common.LearningData;

// Objective function is changed from f(X) to f(X) + l2coef * |X|^2
public class StochasticGradientDescent extends AbstractOnlineOptimizationMethod {
    private double l2coef;
    private double lr;

    public StochasticGradientDescent() {
        super();
        maxIter = 50;
        l2coef = 0.0;
        lr = 0.001;
        tol = 5.0;
    }

    public StochasticGradientDescent(int maxIter, double l2coef, double learningRate, double tol) {
        super();
        this.maxIter = maxIter;
        this.l2coef = l2coef;
        this.lr = learningRate;
        this.tol = tol;
    }

    public double update(LearningModel model, LearningData learningData) {
        double objVal = 0.0;
        L2Regularizer l2term = new L2Regularizer();
        if (l2coef != 0.0) {
            objVal += SolverUtilities.getL2RegularizationObjective(model, l2term, l2coef);
        }
        ObjectiveFunction objFunc = model.getObjectiveFunction();
        learningData.startNewIteration();
        objVal += SolverUtilities.stochasticGradientDescentUpdate(model, objFunc, learningData, l2term, l2coef, lr);
        return objVal;
    }
}
