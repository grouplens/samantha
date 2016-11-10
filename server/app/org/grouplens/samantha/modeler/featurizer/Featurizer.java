package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.common.LearningInstance;

public interface Featurizer {
    LearningInstance featurize(JsonNode entity, boolean update);
}
