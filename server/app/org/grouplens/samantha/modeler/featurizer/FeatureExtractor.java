package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.space.IndexSpace;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface FeatureExtractor extends Serializable {
    Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                       IndexSpace indexSpace);
}
