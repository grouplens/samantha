package org.grouplens.samantha.modeler.boosting;

import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.PredictiveModel;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.modeler.featurizer.StandardLearningInstance;

import java.util.List;

public interface GBCent extends PredictiveModel {
    SVDFeature getSVDFeatureModel();
    LearningData getLearningData(List<StandardLearningInstance> treeInstances);
    PredictiveModel getNumericalTree(int treeIdx);
    void setNumericalTree(int treeIdx, PredictiveModel tree);
}
