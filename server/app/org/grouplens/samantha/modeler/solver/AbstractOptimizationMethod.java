package org.grouplens.samantha.modeler.solver;

import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.PredictiveModel;
import org.grouplens.samantha.server.exception.BadRequestException;

abstract public class AbstractOptimizationMethod implements OptimizationMethod {
    final protected double tol;
    final protected int maxIter;
    final protected int minIter;

    public AbstractOptimizationMethod(double tol, int maxIter, int minIter) {
        this.tol = tol;
        this.maxIter = maxIter;
        this.minIter = minIter;
    }

    protected double update(LearningModel model, LearningData learningData) {
        throw new BadRequestException("update method is not supported.");
    }

    public double minimize(LearningModel learningModel, LearningData learningData, LearningData validData) {
        TerminationCriterion learnCrit = new TerminationCriterion(tol, maxIter, minIter);
        TerminationCriterion validCrit = null;
        if (validData != null) {
            validCrit = new TerminationCriterion(tol, maxIter, minIter);
        }
        double learnObjVal = 0.0;
        while (learnCrit.keepIterate()) {
            if (validCrit != null && !(validCrit.keepIterate())) {
                break;
            }
            learnObjVal = this.update(learningModel, learningData);
            learnCrit.addIteration(AbstractOptimizationMethod.class.toString()
                    + " -- Learning", learnObjVal);
            if (validData != null) {
                double validObjVal = SolverUtilities.evaluate(learningModel, validData);
                validCrit.addIteration(AbstractOptimizationMethod.class.toString()
                        + " -- Validating", validObjVal);
            }
        }
        return learnObjVal;
    }

    public void learn(PredictiveModel model, LearningData learningData, LearningData validData) {
        LearningModel learningModel = (LearningModel) model;
        minimize(learningModel, learningData, validData);
    }
}
