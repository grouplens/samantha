package org.grouplens.samantha.modeler.solver;

import com.google.inject.ImplementedBy;
import org.grouplens.samantha.modeler.common.LearningData;

@ImplementedBy(StochasticGradientDescent.class)
public interface OnlineOptimizationMethod extends OptimizationMethod {
    double update(LearningModel learningModel, LearningData learningData);
}
