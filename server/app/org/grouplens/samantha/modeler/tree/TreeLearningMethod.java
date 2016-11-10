package org.grouplens.samantha.modeler.tree;

import com.google.inject.ImplementedBy;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningMethod;

@ImplementedBy(ExactGreedy.class)
public interface TreeLearningMethod extends LearningMethod {
    void learn(DecisionTree tree, LearningData learningData);
}
