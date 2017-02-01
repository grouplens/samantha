package org.grouplens.samantha.modeler.common;

import java.util.List;

/**
 * The interface representing a collection of {@link LearningInstance}.
 *
 * Typically, this is responsible in reading in data from
 * a {@link org.grouplens.samantha.modeler.dao.EntityDAO EntityDAO} and featurize each data point into
 * {@link LearningInstance}. So, highly likely, LearningData depends on
 * {@link org.grouplens.samantha.modeler.featurizer.Featurizer Featurizer}.
 */
public interface LearningData {
    /**
     * Get the next list of learning instances in the learning data.
     *
     * For each call of this method, it's supposed to return a list of {@link LearningInstance}s. Some implementations
     * group the learning instances together and load the group into the returned list.
     * If the underlying data reaches the end, it should return an empty list.
     *
     * @return if the returned list has length 0, it means the LearningData reaches the end.
     */
    List<LearningInstance> getLearningInstance();

    /**
     * Start a new iteration of the learning data so that {@link #getLearningInstance()} can return {@link LearningInstance}s
     * from the beginning of the data.
     */
    void startNewIteration();
}
