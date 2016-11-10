package org.grouplens.samantha.modeler.solver;

import org.grouplens.samantha.modeler.common.LearningData;

abstract public class AbstractOnlineOptimizationMethod extends AbstractOptimizationMethod implements OnlineOptimizationMethod {

    public double minimize(LearningModel learningModel, LearningData learningData, LearningData validData) {
        TerminationCriterion learnCrit = new TerminationCriterion(tol, maxIter);
        TerminationCriterion validCrit = null;
        if (validData != null) {
            validCrit = new TerminationCriterion(tol, maxIter);
        }
        double learnObjVal = 0.0;
        while (learnCrit.keepIterate()) {
            if (validCrit != null && !(validCrit.keepIterate())) {
                break;
            }
            learnObjVal = update(learningModel, learningData);
            learnCrit.addIteration(AbstractOnlineOptimizationMethod.class.toString()
                    + " -- Learning", learnObjVal);
            if (validData != null) {
                double validObjVal = SolverUtilities.evaluate(learningModel, validData);
                validCrit.addIteration(AbstractOnlineOptimizationMethod.class.toString()
                        + " -- Validating", validObjVal);
            }
        }
        return learnObjVal;
    }

}
