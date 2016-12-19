package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.space.IndexSpace;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

//TODO: warn if attr is not present
//TODO: currently mostly feature extractors are using attrName in data to be the key internal representation, consider separate them and use attrName as default
public interface FeatureExtractor extends Serializable {
    Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                       IndexSpace indexSpace);
}
