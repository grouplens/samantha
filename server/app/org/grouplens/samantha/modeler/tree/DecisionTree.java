package org.grouplens.samantha.modeler.tree;

import com.fasterxml.jackson.databind.JsonNode;
import it.unimi.dsi.fastutil.ints.IntList;
import org.grouplens.samantha.modeler.featurizer.Feature;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.common.PredictiveModel;
import org.grouplens.samantha.modeler.featurizer.StandardLearningInstance;

import java.util.List;

public interface DecisionTree extends PredictiveModel {
    StandardLearningInstance getLearningInstance(LearningInstance ins);
    SplittingCriterion createSplittingCriterion();
    int predictLeaf(JsonNode entity);

    /**
     * @return The newly created node index.
     */
    int createNode(int parentNode, boolean left, IntList relevant,
                    List<double[]> respList, Feature bestSplit);
}
