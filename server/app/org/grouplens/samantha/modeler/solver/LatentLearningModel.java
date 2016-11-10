package org.grouplens.samantha.modeler.solver;

import org.grouplens.samantha.modeler.common.LearningInstance;

public interface LatentLearningModel {
    double expectation(LearningInstance ins);
    LearningModel maximization();
}
