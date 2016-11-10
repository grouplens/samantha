package org.grouplens.samantha.modeler.boosting;

import org.grouplens.samantha.modeler.common.LearningMethod;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.SpaceProducer;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.grouplens.samantha.modeler.tree.RegressionCriterion;
import org.grouplens.samantha.modeler.tree.TreeKey;

import javax.inject.Inject;
import java.util.List;

public class GBDTProducer {
    @Inject
    private RegressionCriterion criterion;
    @Inject
    private SpaceProducer spaceProducer;

    @Inject
    private GBDTProducer() {}

    public GBDTProducer(RegressionCriterion criterion, SpaceProducer spaceProducer) {
        this.criterion = criterion;
        this.spaceProducer = spaceProducer;
    }

    public GBDT createGBRT(String modelName,
                           ObjectiveFunction objectiveFunction,
                           LearningMethod method,
                           List<String> features,
                           List<FeatureExtractor> featureExtractors,
                           String labelName, String weightName) {
        IndexSpace indexSpace = spaceProducer.getIndexSpace(modelName);
        indexSpace.requestKeyMap(TreeKey.TREE.get());
        VariableSpace variableSpace = spaceProducer.getVariableSpace(modelName);
        return new GBDT(modelName, criterion, method, indexSpace, variableSpace,
                objectiveFunction, features, featureExtractors, labelName, weightName);
    }
}
