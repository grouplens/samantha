package org.grouplens.samantha.modeler.boosting;

import org.grouplens.samantha.modeler.common.LearningMethod;
import org.grouplens.samantha.modeler.common.PredictiveModel;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;

public interface BoostedPredictiveModel extends PredictiveModel {
    ObjectiveFunction getObjectiveFunction();
    PredictiveModel getPredictiveModel();
    LearningMethod getLearningMethod();
    void addPredictiveModel(PredictiveModel model);
    void setBestIteration(int iter);
}
