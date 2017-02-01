package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.common.LearningInstance;

/**
 * The interface representing the process of transforming a raw data point in JSON to be a numerical {@link LearningInstance}.
 */
public interface Featurizer {
    /**
     * Featurize the raw data point/entity in JSON format to be a {@link LearningInstance}, typically by calling a series
     * of {@link FeatureExtractor}s.
     *
     * @param entity The JSON format representation of the raw data point.
     * @param update whether update the model (index space etc.). In training a model, this should be true, while in
     *               predicting, this should be false.
     * @return The numerical learning instance representation of the data point.
     */
    LearningInstance featurize(JsonNode entity, boolean update);
}
