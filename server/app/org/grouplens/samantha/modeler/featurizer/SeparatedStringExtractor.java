package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.space.IndexSpace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeparatedStringExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private final String indexName;
    private final String attrName;
    private final String feaName;
    private final String separator;

    public SeparatedStringExtractor(String indexName,
                                    String attrName,
                                    String feaName,
                                    String separator) {
        this.indexName = indexName;
        this.attrName = attrName;
        this.feaName = feaName;
        this.separator = separator;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        Map<String, List<Feature>> feaMap = new HashMap<>();
        if (entity.has(attrName)) {
            List<Feature> features = new ArrayList<>();
            String attr = entity.get(attrName).asText();
            String[] fields = attr.split(separator);
            double val = 0.0;
            if (fields.length > 0) {
                val = 1.0 / Math.sqrt(fields.length);
            }
            for (String field : fields) {
                String key = FeatureExtractorUtilities.composeKey(attrName, field);
                FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(features, update,
                        indexSpace, indexName, key, val);
            }
            feaMap.put(feaName, features);
        }
        return feaMap;
    }
}
