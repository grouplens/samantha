package org.grouplens.samantha.modeler.boosting;

import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.common.PredictiveModel;
import org.grouplens.samantha.modeler.featurizer.*;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureModel;
import org.grouplens.samantha.modeler.tree.*;

import java.util.List;

//TODO: change this to be an interface so that GB-CENT can depend on other implementations, such as org.grouplens.samantha.xgboost
public class RegressionTreeGBCent extends AbstractGBCent implements PredictiveModel, GBCent {
    private static final long serialVersionUID = 1L;
    private final String modelName;
    private final VariableSpace variableSpace;
    private final RegressionCriterion criterion;

    public RegressionTreeGBCent(String modelName, List<FeatureExtractor> treeExtractors,
                                List<String> treeFeatures, String labelName, String weightName,
                                IndexSpace indexSpace, VariableSpace variableSpace,
                                SVDFeatureModel svdFeatureModel, RegressionCriterion criterion) {
        super(indexSpace, treeExtractors, treeFeatures, labelName, weightName, svdFeatureModel);
        this.modelName = modelName;
        this.variableSpace = variableSpace;
        this.criterion = criterion;
    }

    public double predict(LearningInstance ins) {
        GBCentLearningInstance centIns = (GBCentLearningInstance) ins;
        double pred = svdfeaModel.predict(centIns.svdfeaIns);
        for (Feature feature : centIns.svdfeaIns.getBiasFeatures()) {
            int idx = feature.getIndex();
            if (idx < trees.size()) {
                PredictiveModel tree = trees.get(idx);
                if (tree != null) {
                    pred += tree.predict(centIns.treeIns);
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
}
