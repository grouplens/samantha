package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.grouplens.samantha.modeler.space.IndexSpace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PairwiseInteractionExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private final String indexName;
    private final List<String> attrNames;
    private final boolean sigmoid;

    public PairwiseInteractionExtractor(String indexName, List<String> attrNames, boolean sigmoid) {
        this.indexName = indexName;
        this.attrNames = attrNames;
        this.sigmoid = sigmoid;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        Map<String, List<Feature>> feaMap = new HashMap<>();
        UnivariateFunction sig = new SelfPlusOneRatioFunction();
        for (int i=0; i<attrNames.size(); i++) {
            String attrNameLeft = attrNames.get(i);
            for (int j=i+1; j<attrNames.size(); j++) {
                String attrNameRight = attrNames.get(j);
                if (entity.has(attrNameLeft) && entity.has(attrNameRight)) {
                    double valLeft = entity.get(attrNameLeft).asDouble();
                    double valRight = entity.get(attrNameRight).asDouble();
                    if (sigmoid) {
                        valLeft = sig.value(valLeft);
                        valRight = sig.value(valRight);
                    }
                    double value = valLeft * valRight;
                    List<Feature> feaList = new ArrayList<>();
                    String key = FeatureExtractorUtilities.composeKey(attrNameLeft, attrNameRight);
                    FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(feaList, update,
                            indexSpace, indexName, key, value);
                    feaMap.put(attrNameLeft + ":" + attrNameRight, feaList);
                }
            }
        }
        return feaMap;
    }
}
