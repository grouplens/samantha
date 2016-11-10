package org.grouplens.samantha.modeler.boosting;

import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;

public class GBCentSVDFeatureData implements LearningData {

    private final LearningData learningData;

    GBCentSVDFeatureData(LearningData learningData) {
        this.learningData = learningData;
    }

    public void startNewIteration() {
        learningData.startNewIteration();
    }

    public LearningInstance getLearningInstance() {
        LearningInstance ins = learningData.getLearningInstance();
        if (ins == null) {
            return null;
        } else {
            GBCentLearningInstance centIns = (GBCentLearningInstance) ins;
            return centIns.svdfeaIns;
        }
    }
}
