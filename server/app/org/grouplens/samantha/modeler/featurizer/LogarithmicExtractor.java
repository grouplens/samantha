package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Log10;
import org.grouplens.samantha.modeler.space.IndexSpace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogarithmicExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private final String indexName;
    private final String attrName;
    private final String feaName;

    public LogarithmicExtractor(String indexName, String attrName, String feaName) {
        this.attrName = attrName;
        this.feaName = feaName;
        this.indexName = indexName;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        Map<String, List<Feature>> feaMap = new HashMap<>();
        if (entity.has(attrName)) {
            List<Feature> feaList = new ArrayList<>();
            double val = entity.get(attrName).asDouble();
            UnivariateFunction log10 = new Log10();
            FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(feaList, update,
                    indexSpace, indexName, attrName, log10.value(val + 1.0));
            feaMap.put(feaName, feaList);
        }
        return feaMap;
    }
}
