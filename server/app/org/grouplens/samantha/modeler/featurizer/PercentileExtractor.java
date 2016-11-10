package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.space.IndexSpace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PercentileExtractor {
    final private PercentileModel percentileModel;
    final private String feaName;
    final private String attrName;
    final private String indexName;

    public PercentileExtractor(PercentileModel percentileModel, String attrName,
                               String feaName, String indexName) {
        this.feaName = feaName;
        this.attrName = attrName;
        this.indexName = indexName;
        this.percentileModel = percentileModel;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        Map<String, List<Feature>> feaMap = new HashMap<>();
        if (entity.has(attrName)) {
            List<Feature> feaList = new ArrayList<>();
            double val = entity.get(attrName).asDouble();
            FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(feaList, update,
                    indexSpace, indexName, attrName, percentileModel.getPercentile(attrName, val));
            feaMap.put(feaName, feaList);
        }
        return feaMap;
    }
}
