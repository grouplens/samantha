package org.grouplens.samantha.modeler.featurizer;

import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;

import java.util.List;

public class StandardListLearningData implements LearningData {
    private final List<StandardLearningInstance> instances;
    private int idx = 0;

    public StandardListLearningData(List<StandardLearningInstance> instances) {
        this.instances = instances;
    }

    public LearningInstance getLearningInstance() {
        if (idx < instances.size()) {
            return instances.get(idx++);
        } else {
            return null;
        }
    }

    public void startNewIteration() {
        idx = 0;
    }
}
