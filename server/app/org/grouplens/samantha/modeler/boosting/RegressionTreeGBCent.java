package org.grouplens.samantha.modeler.boosting;

import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.common.PredictiveModel;
import org.grouplens.samantha.modeler.featurizer.*;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureInstance;
import org.grouplens.samantha.modeler.tree.*;

import java.util.List;

public class RegressionTreeGBCent extends AbstractGBCent implements PredictiveModel, GBCent {
    private static final long serialVersionUID = 1L;
    private final String modelName;
    private final VariableSpace variableSpace;
    private final RegressionCriterion criterion;

    public RegressionTreeGBCent(String modelName, List<FeatureExtractor> treeExtractors,
                                List<String> treeFeatures, List<String> groupKeys,
                                String labelName, String weightName,
                                IndexSpace indexSpace, VariableSpace variableSpace,
                                SVDFeature svdFeature, RegressionCriterion criterion) {
        super(indexSpace, treeExtractors, treeFeatures, labelName, weightName, groupKeys, svdFeature);
        this.modelName = modelName;
        this.variableSpace = variableSpace;
        this.criterion = criterion;
    }

    public double predict(LearningInstance ins) {
        GBCentLearningInstance centIns = (GBCentLearningInstance) ins;
        SVDFeatureInstance svdfeaIns = centIns.getSvdfeaIns();
        StandardLearningInstance treeIns = centIns.getTreeIns();
        double pred = svdfeaModel.predict(svdfeaIns);
        for (Feature feature : svdfeaIns.getBiasFeatures()) {
            int idx = feature.getIndex();
            if (idx < trees.size()) {
                PredictiveModel tree = trees.get(idx);
                if (tree != null) {
                    pred += tree.predict(treeIns);
                }
            }
        }
        return pred;
    }

    //TODO: for updating trees, this needs to create a temporary tree instead of setting the variableSpace directly
    public PredictiveModel getNumericalTree(int treeIdx) {
        String treeName = FeatureExtractorUtilities.composeKey(modelName, Integer.toString(treeIdx));
        variableSpace.requestVectorVar(treeName, 0, RegressionTree.nodeSize, 0.0, false, false);
        RegressionTree tree = new RegressionTree(treeName, criterion, indexSpace, variableSpace,
                features, featureExtractors, labelName, weightName);
        return tree;
    }

    //TODO: for updating trees, this needs to copy out the learned tree, i.e. RegressionTree needs to support
    //TODO:     copying constructor
    //public void setNumericalTree(int treeIdx, PredictiveModel tree);

    public void publishModel() {
        indexSpace.publishSpaceVersion();
        variableSpace.publishSpaceVersion();
    }
}
