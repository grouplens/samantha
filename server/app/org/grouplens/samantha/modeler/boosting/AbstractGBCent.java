package org.grouplens.samantha.modeler.boosting;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.common.PredictiveModel;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureInstance;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureModel;
import org.grouplens.samantha.modeler.featurizer.StandardFeaturizer;
import org.grouplens.samantha.modeler.featurizer.StandardLearningInstance;

import java.util.ArrayList;
import java.util.List;

abstract public class AbstractGBCent extends StandardFeaturizer implements GBCent {
    protected final SVDFeatureModel svdfeaModel;
    protected final List<PredictiveModel> trees = new ArrayList<>();

    public AbstractGBCent(IndexSpace indexSpace, List<FeatureExtractor> featureExtractors,
                          List<String> features, String labelName, String weightName,
                          SVDFeatureModel svdfeaModel) {
        super(indexSpace, featureExtractors, features, labelName, weightName);
        this.svdfeaModel = svdfeaModel;
    }

    public LearningInstance featurize(JsonNode entity, boolean update) {
        SVDFeatureInstance svdIns = (SVDFeatureInstance) svdfeaModel.featurize(entity, update);
        StandardLearningInstance treeIns = (StandardLearningInstance) super.featurize(entity, update);
        return new GBCentLearningInstance(svdIns, treeIns);
    }

    public SVDFeatureModel getSVDFeatureModel() {
        return this.svdfeaModel;
    }

    public void setNumericalTree(int treeIdx, PredictiveModel tree) {
        while (trees.size() < treeIdx + 1) {
            trees.add(null);
        }
        trees.set(treeIdx, tree);
    }
}
