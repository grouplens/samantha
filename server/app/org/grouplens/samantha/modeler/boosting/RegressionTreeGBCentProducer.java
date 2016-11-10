package org.grouplens.samantha.modeler.boosting;

import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.SpaceProducer;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureModel;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureModelProducer;
import org.grouplens.samantha.modeler.tree.RegressionCriterion;
import org.grouplens.samantha.modeler.tree.TreeKey;

import javax.inject.Inject;
import java.util.List;

public class RegressionTreeGBCentProducer {
    @Inject
    private RegressionCriterion criterion;
    @Inject
    private SpaceProducer spaceProducer;

    @Inject
    private RegressionTreeGBCentProducer() {}

    public RegressionTreeGBCentProducer(RegressionCriterion criterion, SpaceProducer spaceProducer) {
        this.criterion = criterion;
        this.spaceProducer = spaceProducer;
    }

    public RegressionTreeGBCent createGBCent(String modelName, String labelName, String weightName,
                                             List<FeatureExtractor> svdfeaExtractors,
                                             List<FeatureExtractor> treeExtractors,
                                             List<String> biasFeas, List<String> ufactFeas,
                                             List<String> ifactFeas, List<String> treeFeas,
                                             int factDim, ObjectiveFunction objectiveFunction) {
        IndexSpace indexSpace = spaceProducer.getIndexSpace(modelName);
        indexSpace.requestKeyMap(TreeKey.TREE.get());
        VariableSpace variableSpace = spaceProducer.getVariableSpace(modelName);
        SVDFeatureModelProducer svdfeaProducer = new SVDFeatureModelProducer(spaceProducer);
        SVDFeatureModel svdfeaModel = svdfeaProducer.createSVDFeatureModel(modelName, biasFeas, ufactFeas,
                ifactFeas, labelName, weightName, svdfeaExtractors, factDim, objectiveFunction);
        return new RegressionTreeGBCent(modelName, treeExtractors, treeFeas, labelName, weightName,
                indexSpace, variableSpace, svdfeaModel, criterion);
    }

    public RegressionTreeGBCent createGBCentWithSVDFeatureModel(String modelName, List<String> treeFeas,
                                                                List<FeatureExtractor> treeExtractors,
                                                                SVDFeatureModel svdfeaModel) {
        IndexSpace indexSpace = spaceProducer.getIndexSpace(modelName);
        indexSpace.requestKeyMap(TreeKey.TREE.get());
        VariableSpace variableSpace = spaceProducer.getVariableSpace(modelName);
        String labelName = svdfeaModel.getLabelName();
        String weightName = svdfeaModel.getWeightName();
        return new RegressionTreeGBCent(modelName, treeExtractors, treeFeas, labelName, weightName,
                indexSpace, variableSpace, svdfeaModel, criterion);
    }
}
