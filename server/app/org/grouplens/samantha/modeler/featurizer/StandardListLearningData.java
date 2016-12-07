package org.grouplens.samantha.modeler.featurizer;

import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;

import java.util.ArrayList;
import java.util.List;

public class StandardListLearningData implements LearningData {
    private final List<StandardLearningInstance> instances;
    private int idx = 0;

    //TODO: support grouping learning instance according to group info.
    public StandardListLearningData(List<StandardLearningInstance> instances) {
        this.instances = instances;
    }

    public List<LearningInstance> getLearningInstance() {
        List<LearningInstance> curList = new ArrayList<>(1);
        if (idx < instances.size()) {
            curList.add(instances.get(idx++));
        }
        return curList;
    }

    public void startNewIteration() {
        idx = 0;
    }
}
