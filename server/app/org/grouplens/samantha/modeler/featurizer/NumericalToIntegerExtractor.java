package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.space.IndexSpace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NumericalToIntegerExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private final String indexName;
    private final String attrName;
    private final String feaName;
    private final double multiplier;

    public NumericalToIntegerExtractor(String indexName,
                                       String attrName,
                                       String feaName, 
                                       double multiplier) {
        this.indexName = indexName;
        this.attrName = attrName;
        this.feaName = feaName;
        this.multiplier = multiplier;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        Map<String, List<Feature>> feaMap = new HashMap<>();
        if (entity.has(attrName)) {
            List<Feature> features = new ArrayList<>();
            double val = entity.get(attrName).asDouble();
            val *= multiplier;
            String key = FeatureExtractorUtilities.composeKey(attrName, 
                Integer.valueOf((int) val).toString());
            FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(features, update,
                    indexSpace, indexName, key, 1.0);
            feaMap.put(feaName, features);
        }
        return feaMap;
    }
}
