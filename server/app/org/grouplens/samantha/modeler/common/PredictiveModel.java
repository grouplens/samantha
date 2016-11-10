package org.grouplens.samantha.modeler.common;

import java.io.Serializable;

public interface PredictiveModel extends Serializable {
    double predict(LearningInstance ins);
}
