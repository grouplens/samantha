package org.grouplens.samantha.modeler.tree;

import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.SpaceProducer;
import org.grouplens.samantha.modeler.space.VariableSpace;

import javax.inject.Inject;
import java.util.List;

public class RegressionTreeProducer {
    @Inject
    private RegressionCriterion criterion;
    @Inject
    private SpaceProducer spaceProducer;

    @Inject
    private RegressionTreeProducer() {}

    public RegressionTreeProducer(RegressionCriterion criterion, SpaceProducer spaceProducer) {
        this.criterion = criterion;
        this.spaceProducer = spaceProducer;
    }

    public RegressionTree createRegressionTree(String treeName,
                                               List<String> features,
                                               List<FeatureExtractor> featureExtractors,
                                               String labelName, String weightName) {
        IndexSpace indexSpace = spaceProducer.getIndexSpace(treeName);
        indexSpace.requestKeyMap(TreeKey.TREE.get());
        VariableSpace variableSpace = spaceProducer.getVariableSpace(treeName);
        variableSpace.requestVectorVar(treeName, 0, RegressionTree.nodeSize, 0.0, false, false);
        return new RegressionTree(treeName, criterion, indexSpace, variableSpace,
                features, featureExtractors, labelName, weightName);
    }
}
