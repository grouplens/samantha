package org.grouplens.samantha.modeler.svdfeature;

import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;

import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.SpaceProducer;
import org.grouplens.samantha.modeler.space.VariableSpace;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

public class SVDFeatureModelProducer {
    @Inject private SpaceProducer spaceProducer;

    @Inject
    private SVDFeatureModelProducer() {}

    public SVDFeatureModelProducer(SpaceProducer spaceProducer) {
        this.spaceProducer = spaceProducer;
    }

    private IndexSpace getIndexSpace(String spaceName) {
        IndexSpace indexSpace = spaceProducer.getIndexSpace(spaceName);
        indexSpace.requestKeyMap(SVDFeatureKey.BIASES.get());
        indexSpace.requestKeyMap(SVDFeatureKey.FACTORS.get());
        return indexSpace;
    }

    private VariableSpace getVariableSpace(String spaceName,
                                           int biasSize, int factSize, int factDim) {
        VariableSpace variableSpace = spaceProducer.getVariableSpace(spaceName);
        variableSpace.requestScalarVar(SVDFeatureKey.BIASES.get(), biasSize, 0.0, false, false);
        variableSpace.requestScalarVar(SVDFeatureKey.SUPPORT.get(), biasSize, 0.0, false, false);
        variableSpace.requestVectorVar(SVDFeatureKey.FACTORS.get(), factSize, factDim, 0.0, true, false);
        return variableSpace;
    }

    public SVDFeatureModel createSVDFeatureModel(String modelName,
                                                 List<String> biasFeas,
                                                 List<String> ufactFeas,
                                                 List<String> ifactFeas,
                                                 String labelName,
                                                 String weightName,
                                                 List<FeatureExtractor> featureExtractors,
                                                 int factDim,
                                                 ObjectiveFunction objectiveFunction) {
        IndexSpace indexSpace = getIndexSpace(modelName);
        VariableSpace variableSpace = getVariableSpace(modelName, 0, 0, factDim);
        return new SVDFeatureModel(biasFeas, ufactFeas, ifactFeas, labelName, weightName, featureExtractors,
                factDim, objectiveFunction, indexSpace, variableSpace);
    }

    public SVDFeatureModel createSVDFeatureModelForInstanceDAO(String modelName,
                                                               int biasSize, int factSize, int factDim,
                                                               ObjectiveFunction objectiveFunction) {
        IndexSpace indexSpace = getIndexSpace(modelName);
        VariableSpace variableSpace = getVariableSpace(modelName, biasSize, factSize, factDim);
        return new SVDFeatureModel(Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST,
                "label", "weight", Collections.EMPTY_LIST, factDim, objectiveFunction, indexSpace, variableSpace);
    }
}
