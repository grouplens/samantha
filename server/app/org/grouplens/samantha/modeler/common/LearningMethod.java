package org.grouplens.samantha.modeler.common;

public interface LearningMethod {
    void learn(PredictiveModel model, LearningData learningData, LearningData validData);
}
