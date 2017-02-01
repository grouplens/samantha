package org.grouplens.samantha.modeler.common;

import org.grouplens.samantha.modeler.space.SpaceModel;

/**
 * The interface representing a predictive model, which takes in an {@link LearningInstance} and produces a value.
 */
public interface PredictiveModel extends SpaceModel {
    /**
     * Make prediction on a {@link LearningInstance} or data point.
     *
     * Currently, it is returning a double value. It will be extended to be more natural for classification in the future,
     * although class label can be represented as a double, too.
     *
     * @param ins the learning instance to make prediction on.
     * @return the predicted value based on the model.
     */
    double predict(LearningInstance ins);
}
