package org.grouplens.samantha.modeler.common;

/**
 * The interface for representing the algorithm/method of learning a {@link PredictiveModel}.
 *
 * This can be an optimization method or greedy tree learning method etc. This method is critical for a standard
 * boosting procedure. I.e. ideally, we should be able to have any predictive models and boost them together following
 * the standard boosting procedure. See {@link org.grouplens.samantha.modeler.boosting.GradientBoostingMachine GradientBoostingMachine}
 * for an example.
 */
public interface LearningMethod {
    /**
     * Learn a predictive model based on the input learning data sets.
     *
     * @param model the predictive model to learn.
     * @param learningData the learning data to use to learn the model, i.e. training data.
     * @param validData the validation data to use while learning, e.g. for early stopping etc. This learning data
     *                  could be null in which the method doesn't take into account the validation process or doesn't
     *                  rely on a separate data set to prevent over-fitting.
     */
    void learn(PredictiveModel model, LearningData learningData, LearningData validData);
}
