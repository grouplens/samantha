package org.grouplens.samantha.modeler.reinforce;

import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.SpaceProducer;
import org.grouplens.samantha.modeler.space.VariableSpace;

import javax.inject.Inject;
import java.util.List;

public class LinearUCBProducer {
    @Inject private SpaceProducer spaceProducer;

    @Inject
    private LinearUCBProducer() {}

    public LinearUCBProducer(SpaceProducer spaceProducer) {
        this.spaceProducer = spaceProducer;
    }

    private IndexSpace getIndexSpace(String spaceName) {
        IndexSpace indexSpace = spaceProducer.getIndexSpace(spaceName);
        indexSpace.requestKeyMap(LinearUCBKey.BIASES.get());
        return indexSpace;
    }

    private VariableSpace getVariableSpace(String spaceName) {
        VariableSpace variableSpace = spaceProducer.getVariableSpace(spaceName);
        variableSpace.requestScalarVar(LinearUCBKey.B.get(), 0, 0.0, false, false);
        variableSpace.requestVectorVar(LinearUCBKey.A.get(), 0, 0, 0.0, true, false);
        return variableSpace;
    }

    public LinearUCB createLinearUCBModel(String modelName,
                                          List<String> features,
                                          int numMainFeatures,
                                          String labelName,
                                          String weightName,
                                          double alpha, double lambda,
                                          List<FeatureExtractor> featureExtractors) {
        IndexSpace indexSpace = getIndexSpace(modelName);
        VariableSpace variableSpace = getVariableSpace(modelName);
        return new LinearUCB(lambda, alpha, features, numMainFeatures, labelName, weightName, featureExtractors,
                indexSpace, variableSpace);
    }
}
