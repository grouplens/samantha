package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.space.IndexSpace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConstantOneExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private final String indexName;
    private final String attrName;
    private final String feaName;

    public ConstantOneExtractor(String indexName, String attrName, String feaName) {
        this.attrName = attrName;
        this.feaName = feaName;
        this.indexName = indexName;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        List<Feature> feaList = new ArrayList<>();
        FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(feaList, update,
                indexSpace, indexName, attrName, 1.0);
        Map<String, List<Feature>> feaMap = new HashMap<>();
        feaMap.put(feaName, feaList);
        return feaMap;
    }
}
