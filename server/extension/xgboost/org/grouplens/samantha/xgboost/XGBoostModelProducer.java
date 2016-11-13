package org.grouplens.samantha.xgboost;

import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.SpaceProducer;
import org.grouplens.samantha.modeler.tree.TreeKey;

import javax.inject.Inject;
import java.util.List;

public class XGBoostModelProducer {
    @Inject
    private SpaceProducer spaceProducer;

    @Inject
    public XGBoostModelProducer() {}

    private IndexSpace getIndexSpace(String spaceName) {
        IndexSpace indexSpace = spaceProducer.getIndexSpace(spaceName);
        indexSpace.requestKeyMap(TreeKey.TREE.get());
        return indexSpace;
    }

    public XGBoostModel createXGBoostModel(String modelName, List<FeatureExtractor> featureExtractors,
                                           List<String> features, String labelName, String weightName) {
        IndexSpace indexSpace = getIndexSpace(modelName);
        return new XGBoostModel(indexSpace, featureExtractors, features, labelName, weightName);
    }
}
