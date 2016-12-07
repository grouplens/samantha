package org.grouplens.samantha.modeler.boosting;

import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;

import java.util.ArrayList;
import java.util.List;

public class GBCentSVDFeatureData implements LearningData {

    private final LearningData learningData;

    GBCentSVDFeatureData(LearningData learningData) {
        this.learningData = learningData;
    }

    public void startNewIteration() {
        learningData.startNewIteration();
    }

    public List<LearningInstance> getLearningInstance() {
        List<LearningInstance> instances = learningData.getLearningInstance();
        if (instances.size() == 0) {
            return instances;
        }
        List<LearningInstance> curList = new ArrayList<>(instances.size());
        for (LearningInstance ins : instances) {
            GBCentLearningInstance centIns = (GBCentLearningInstance) ins;
            curList.add(centIns.getSvdfeaIns());
        }
        return curList;
    }
}
