package org.grouplens.samantha.modeler.solver;

import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.common.PredictiveModel;

abstract public class AbstractOptimizationMethod implements OptimizationMethod {
    protected double tol;
    protected int maxIter;

    public void learn(PredictiveModel model, LearningData learningData, LearningData validData) {
        LearningModel learningModel = (LearningModel) model;
        minimize(learningModel, learningData, validData);
    }
}
