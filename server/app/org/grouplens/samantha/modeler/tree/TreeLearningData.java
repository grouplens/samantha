package org.grouplens.samantha.modeler.tree;

import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.featurizer.StandardLearningInstance;

import java.util.List;

public class TreeLearningData implements LearningData {
    private final List<StandardLearningInstance> instances;
    private int idx = 0;

    public TreeLearningData(List<StandardLearningInstance> instances) {
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
