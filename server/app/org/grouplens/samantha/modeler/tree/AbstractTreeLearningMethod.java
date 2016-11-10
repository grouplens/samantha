package org.grouplens.samantha.modeler.tree;

import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.PredictiveModel;

abstract public class AbstractTreeLearningMethod implements TreeLearningMethod {
    public void learn(PredictiveModel model, LearningData learningData, LearningData validData) {
        DecisionTree tree = (DecisionTree) model;
        learn(tree, learningData);
    }
}
