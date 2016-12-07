package org.grouplens.samantha.modeler.svdfeature;

import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;

import java.util.ArrayList;
import java.util.List;

public class SVDFeatureInstanceList implements LearningData {
    private int iter = 0;
    private final List<SVDFeatureInstance> insList;

    public SVDFeatureInstanceList(List<SVDFeatureInstance> insList) {
        this.insList = insList;
    }

    public List<LearningInstance> getLearningInstance() {
        List<LearningInstance> instances = new ArrayList<>(1);
        if (iter >= insList.size()) {
            return instances;
        }
        instances.add(insList.get(iter++));
        return instances;
    }

    public void startNewIteration() {
        iter = 0;
    }
}
