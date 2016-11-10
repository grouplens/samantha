package org.grouplens.samantha.modeler.svdfeature;

import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;

import java.util.List;

public class SVDFeatureInstanceList implements LearningData {
    private int iter = 0;
    private final List<SVDFeatureInstance> insList;

    public SVDFeatureInstanceList(List<SVDFeatureInstance> insList) {
        this.insList = insList;
    }

    public LearningInstance getLearningInstance() {
        if (iter >= insList.size()) {
            return null;
        }
        return insList.get(iter++);
    }

    public void startNewIteration() {
        iter = 0;
    }
}
