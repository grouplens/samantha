package org.grouplens.samantha.ephemeral.model;

import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.SpaceMode;
import org.grouplens.samantha.modeler.space.SpaceProducer;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureKey;

import javax.inject.Inject;
import java.util.List;

public class CustomSVDFeatureProducer {
    @Inject private SpaceProducer spaceProducer;

    @Inject
    private CustomSVDFeatureProducer() {}

    public CustomSVDFeatureProducer(SpaceProducer spaceProducer) {
        this.spaceProducer = spaceProducer;
    }

    private IndexSpace getIndexSpace(String spaceName, SpaceMode spaceMode) {
        IndexSpace indexSpace = spaceProducer.getIndexSpace(spaceName, spaceMode);
        indexSpace.requestKeyMap(SVDFeatureKey.BIASES.get());
        indexSpace.requestKeyMap(SVDFeatureKey.FACTORS.get());
        return indexSpace;
    }

    private VariableSpace getVariableSpace(String spaceName, SpaceMode spaceMode,
                                           int biasSize, int factSize, int factDim) {
        VariableSpace variableSpace = spaceProducer.getVariableSpace(spaceName, spaceMode);
        variableSpace.requestScalarVar(SVDFeatureKey.BIASES.get(), biasSize, 0.0, false);
        variableSpace.requestScalarVar(SVDFeatureKey.SUPPORT.get(), biasSize, 0.0, false);
        variableSpace.requestVectorVar(SVDFeatureKey.FACTORS.get(), factSize, factDim, 0.0, true, false);
        return variableSpace;
    }

    public CustomSVDFeature createSVDFeatureModel(String modelName,
                                            SpaceMode spaceMode,
                                            List<String> biasFeas,
                                            List<String> ufactFeas,
                                            List<String> ifactFeas,
                                            String labelName,
                                            String weightName,
                                            List<String> groupKeys,
                                            List<FeatureExtractor> featureExtractors,
                                            int factDim,
                                            ObjectiveFunction objectiveFunction) {
        IndexSpace indexSpace = getIndexSpace(modelName, spaceMode);
        VariableSpace variableSpace = getVariableSpace(modelName, spaceMode, 0, 0, factDim);
        return new CustomSVDFeature(biasFeas, ufactFeas, ifactFeas, labelName, weightName, groupKeys,
                featureExtractors, factDim, objectiveFunction, indexSpace, variableSpace);
    }
}
