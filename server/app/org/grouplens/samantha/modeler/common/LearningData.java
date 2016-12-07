package org.grouplens.samantha.modeler.common;

import java.util.List;

public interface LearningData {
    List<LearningInstance> getLearningInstance();
    void startNewIteration();
}
