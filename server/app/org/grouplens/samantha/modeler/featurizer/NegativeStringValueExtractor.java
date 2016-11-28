package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.space.IndexSpace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * For pairwise ranking learning. E.g. if the negative item attribute is item_neg, 
 * then attrName should be item_neg, toReplace should be _neg.
 */
public class NegativeStringValueExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private final String indexName;
    private final String attrName;
    private final String feaName;
    private final String toReplace;

    public NegativeStringValueExtractor(String indexName,
                                        String attrName,
                                        String feaName, 
                                        String toReplace) {
        this.indexName = indexName;
        this.attrName = attrName;
        this.feaName = feaName;
        this.toReplace = toReplace;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        Map<String, List<Feature>> feaMap = new HashMap<>();
        if (entity.has(attrName)) {
            List<Feature> features = new ArrayList<>();
            String key = FeatureExtractorUtilities.composeKey(
                attrName.replace(toReplace, ""), 
                entity.get(attrName).asText()
            );
            FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(features, update,
                    indexSpace, indexName, key, -1.0);
            feaMap.put(feaName, features);
        }
        return feaMap;
    }
}
