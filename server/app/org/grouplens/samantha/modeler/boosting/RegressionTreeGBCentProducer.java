package org.grouplens.samantha.modeler.boosting;

import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.SpaceMode;
import org.grouplens.samantha.modeler.space.SpaceProducer;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureProducer;
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

    public RegressionTreeGBCent createGBCent(String modelName, SpaceMode spaceMode,
                                             String labelName, String weightName,
                                             List<FeatureExtractor> svdfeaExtractors,
                                             List<FeatureExtractor> treeExtractors,
                                             List<String> biasFeas, List<String> ufactFeas,
                                             List<String> ifactFeas, List<String> treeFeas,
                                             int factDim, ObjectiveFunction objectiveFunction) {
        IndexSpace indexSpace = spaceProducer.getIndexSpace(modelName, spaceMode);
        indexSpace.requestKeyMap(TreeKey.TREE.get());
        VariableSpace variableSpace = spaceProducer.getVariableSpace(modelName, spaceMode);
        SVDFeatureProducer svdfeaProducer = new SVDFeatureProducer(spaceProducer);
        SVDFeature svdfeaModel = svdfeaProducer.createSVDFeatureModel(modelName, spaceMode,
                biasFeas, ufactFeas, ifactFeas, labelName, weightName, svdfeaExtractors,
                factDim, objectiveFunction);
        return new RegressionTreeGBCent(modelName, treeExtractors, treeFeas, labelName, weightName,
                indexSpace, variableSpace, svdfeaModel, criterion);
    }

    public RegressionTreeGBCent createGBCentWithSVDFeatureModel(String modelName, SpaceMode spaceMode,
                                                                List<String> treeFeas,
                                                                List<FeatureExtractor> treeExtractors,
                                                                SVDFeature svdfeaModel) {
        IndexSpace indexSpace = spaceProducer.getIndexSpace(modelName, spaceMode);
        indexSpace.requestKeyMap(TreeKey.TREE.get());
        VariableSpace variableSpace = spaceProducer.getVariableSpace(modelName, spaceMode);
        String labelName = svdfeaModel.getLabelName();
        String weightName = svdfeaModel.getWeightName();
        return new RegressionTreeGBCent(modelName, treeExtractors, treeFeas, labelName, weightName,
                indexSpace, variableSpace, svdfeaModel, criterion);
    }
}
