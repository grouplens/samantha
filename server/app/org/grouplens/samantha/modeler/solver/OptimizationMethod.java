package org.grouplens.samantha.modeler.solver;

import com.google.inject.ImplementedBy;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningMethod;

@ImplementedBy(StochasticGradientDescent.class)
public interface OptimizationMethod extends LearningMethod {
    double minimize(LearningModel model, LearningData learningData, LearningData validData);
}
