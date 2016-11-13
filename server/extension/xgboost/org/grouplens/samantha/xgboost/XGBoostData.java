package org.grouplens.samantha.xgboost;

import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.featurizer.StandardLearningInstance;

import java.util.List;

public class XGBoostData implements LearningData {
    private final List<StandardLearningInstance> instances;
    private int idx = 0;

    public XGBoostData(List<StandardLearningInstance> instances) {
        this.instances = instances;
    }

    public LearningInstance getLearningInstance() {
        if (idx < instances.size()) {
            return new XGBoostInstance(instances.get(idx++));
        } else {
            return null;
        }
    }

    public void startNewIteration() {
        idx = 0;
    }
}
