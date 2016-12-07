package org.grouplens.samantha.modeler.common;

import org.grouplens.samantha.modeler.space.SpaceModel;

public interface PredictiveModel extends SpaceModel {
    double predict(LearningInstance ins);
}
