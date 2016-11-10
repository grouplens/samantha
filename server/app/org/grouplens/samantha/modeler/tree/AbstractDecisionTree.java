package org.grouplens.samantha.modeler.tree;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.featurizer.Featurizer;
import org.grouplens.samantha.modeler.featurizer.StandardFeaturizer;
import org.grouplens.samantha.modeler.featurizer.StandardLearningInstance;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.VariableSpace;

import java.util.List;

abstract public class AbstractDecisionTree implements DecisionTree, Featurizer {
    private final Featurizer featurizer;
    protected final VariableSpace variableSpace;
    protected final List<FeatureExtractor> featureExtractors;
    protected final String labelName;
    protected final String weightName;
    protected final List<String> features;

    protected AbstractDecisionTree(IndexSpace indexSpace, VariableSpace variableSpace, List<String> features,
                                   List<FeatureExtractor> featureExtractors, String labelName,
                                   String weightName) {
        this.featureExtractors = featureExtractors;
        this.variableSpace = variableSpace;
        this.labelName = labelName;
        this.weightName = weightName;
        this.features = features;
        featurizer = new StandardFeaturizer(indexSpace, featureExtractors, features, labelName, weightName);
    }

    public StandardLearningInstance getLearningInstance(LearningInstance ins) {
        return (StandardLearningInstance) ins;
    }

    public LearningInstance featurize(JsonNode entity, boolean update) {
        return featurizer.featurize(entity, update);
    }
}
