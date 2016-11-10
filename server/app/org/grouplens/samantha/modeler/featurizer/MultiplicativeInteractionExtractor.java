package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.grouplens.samantha.modeler.space.IndexSpace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiplicativeInteractionExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private final String indexName;
    private final List<String> attrNames;
    private final String feaName;
    private final boolean sigmoid;

    public MultiplicativeInteractionExtractor(String indexName, List<String> attrNames,
                                              String feaName, boolean sigmoid) {
        this.feaName = feaName;
        this.indexName = indexName;
        this.attrNames = attrNames;
        this.sigmoid = sigmoid;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        Map<String, List<Feature>> feaMap = new HashMap<>();
        double product = 1.0;
        boolean complete = true;
        UnivariateFunction sig = new SelfPlusOneRatioFunction();
        for (String attrName : attrNames) {
            if (entity.has(attrName)) {
                double val = entity.get(attrName).asDouble();
                if (sigmoid) {
                    val = sig.value(val);
                }
                product *= val;
            } else {
                complete = false;
            }
        }
        if (complete) {
            List<Feature> feaList = new ArrayList<>();
            String key = FeatureExtractorUtilities.composeKey(attrNames);
            FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(feaList, update,
                    indexSpace, indexName, key, product);
            feaMap.put(feaName, feaList);
        }
        return feaMap;
    }
}
