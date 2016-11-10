package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.space.IndexSpace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConcatenateStringExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private final String indexName;
    private final String feaName;
    private final List<String> attrNames;

    public ConcatenateStringExtractor(String indexName,
                                      List<String> attrNames,
                                      String feaName) {
        this.indexName = indexName;
        this.feaName = feaName;
        this.attrNames = attrNames;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        Map<String, List<Feature>> feaMap = new HashMap<>();
        String key = FeatureExtractorUtilities.composeConcatenatedKey(entity, attrNames);
        if (!"".equals(key)) {
            List<Feature> features = new ArrayList<>();
            FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(features, update,
                    indexSpace, indexName, key, 1.0);
            feaMap.put(feaName, features);
        }
        return feaMap;
    }
}
