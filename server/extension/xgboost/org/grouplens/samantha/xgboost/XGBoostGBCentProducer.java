package org.grouplens.samantha.xgboost;

import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.SpaceProducer;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureModel;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureModelProducer;
import org.grouplens.samantha.modeler.tree.TreeKey;

import javax.inject.Inject;
import java.util.List;

public class XGBoostGBCentProducer {
    @Inject
    private SpaceProducer spaceProducer;

    @Inject
    private XGBoostGBCentProducer() {}

    public XGBoostGBCentProducer(SpaceProducer spaceProducer) {
        this.spaceProducer = spaceProducer;
    }

    public XGBoostGBCent createGBCent(String modelName, String labelName, String weightName,
                                      List<FeatureExtractor> svdfeaExtractors,
                                      List<FeatureExtractor> treeExtractors,
                                      List<String> biasFeas, List<String> ufactFeas,
                                      List<String> ifactFeas, List<String> treeFeas,
                                      int factDim, ObjectiveFunction objectiveFunction) {
        IndexSpace indexSpace = spaceProducer.getIndexSpace(modelName);
        indexSpace.requestKeyMap(TreeKey.TREE.get());
        SVDFeatureModelProducer svdfeaProducer = new SVDFeatureModelProducer(spaceProducer);
        SVDFeatureModel svdfeaModel = svdfeaProducer.createSVDFeatureModel(modelName, biasFeas, ufactFeas,
                ifactFeas, labelName, weightName, svdfeaExtractors, factDim, objectiveFunction);
        return new XGBoostGBCent(treeExtractors, treeFeas, labelName, weightName,
                indexSpace, svdfeaModel);
    }

    public XGBoostGBCent createGBCentWithSVDFeatureModel(String modelName, List<String> treeFeas,
                                                         List<FeatureExtractor> treeExtractors,
                                                         SVDFeatureModel svdfeaModel) {
        IndexSpace indexSpace = spaceProducer.getIndexSpace(modelName);
        indexSpace.requestKeyMap(TreeKey.TREE.get());
        String labelName = svdfeaModel.getLabelName();
        String weightName = svdfeaModel.getWeightName();
        return new XGBoostGBCent(treeExtractors, treeFeas, labelName, weightName,
                indexSpace, svdfeaModel);
    }
}
