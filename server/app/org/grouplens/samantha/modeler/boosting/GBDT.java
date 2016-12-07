package org.grouplens.samantha.modeler.boosting;

import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.common.LearningMethod;
import org.grouplens.samantha.modeler.common.PredictiveModel;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.grouplens.samantha.modeler.tree.RegressionTree;
import org.grouplens.samantha.modeler.tree.SplittingCriterion;
import org.grouplens.samantha.modeler.featurizer.StandardFeaturizer;

import java.util.ArrayList;
import java.util.List;

public class GBDT extends StandardFeaturizer implements BoostedPredictiveModel {
    private static final long serialVersionUID = 1L;
    final private String modelName;
    final private SplittingCriterion criterion;
    final private VariableSpace variableSpace;
    final private ObjectiveFunction objectiveFunction;
    final private LearningMethod method;
    final private List<RegressionTree> trees = new ArrayList<>();

    /**
     * Directly calling this is discouraged. Use {@link GBDTProducer} instead.
     */
    public GBDT(String modelName,
                SplittingCriterion criterion,
                LearningMethod method,
                IndexSpace indexSpace, VariableSpace variableSpace,
                ObjectiveFunction objectiveFunction,
                List<String> features,
                List<FeatureExtractor> featureExtractors,
                String labelName, String weightName) {
        super(indexSpace, featureExtractors, features, labelName, weightName);
        this.modelName = modelName;
        this.criterion = criterion;
        this.variableSpace = variableSpace;
        this.objectiveFunction = objectiveFunction;
        this.method = method;
    }

    public double predict(LearningInstance ins) {
        double pred = 0.0;
        for (RegressionTree tree : trees) {
            pred += tree.predict(ins);
        }
        return pred;
    }

    public ObjectiveFunction getObjectiveFunction() {
        return this.objectiveFunction;
    }

    public PredictiveModel getPredictiveModel() {
        String treeName = FeatureExtractorUtilities.composeKey(modelName, Integer.toString(trees.size()));
        variableSpace.requestVectorVar(treeName, 0, RegressionTree.nodeSize, 0.0, false, false);
        RegressionTree tree = new RegressionTree(treeName, criterion, indexSpace, variableSpace,
                features, featureExtractors, labelName, weightName);
        return tree;
    }

    public LearningMethod getLearningMethod() {
        return method;
    }

    public void addPredictiveModel(PredictiveModel model) {
        RegressionTree tree = (RegressionTree) model;
        trees.add(tree);
    }

    public void setBestIteration(int bestIter) {
        int len = trees.size();
        for (int i=len - 1; i>=0; i--) {
            if (i > bestIter) {
                trees.remove(i);
            } else {
                break;
            }
        }
    }

    public void publishModel() {
        indexSpace.publishSpaceVersion();
        variableSpace.publishSpaceVersion();
    }
}
