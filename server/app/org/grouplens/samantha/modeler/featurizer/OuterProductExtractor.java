package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Log10;
import org.grouplens.samantha.modeler.space.IndexSpace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OuterProductExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private final String indexName;
    private final List<String> attrNames;
    private final String feaName;
    private final boolean sigmoid;

    public OuterProductExtractor(String indexName, List<String> attrNames, String feaName, boolean sigmoid) {
        this.feaName = feaName;
        this.indexName = indexName;
        this.attrNames = attrNames;
        this.sigmoid = sigmoid;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        Map<String, List<Feature>> feaMap = new HashMap<>();
        List<Feature> feaList = new ArrayList<>();
        for (String leftName : attrNames) {
            for (String rightName : attrNames) {
                List<String> keyNames = Lists.newArrayList(leftName, rightName);
                String key = FeatureExtractorUtilities.composeKey(keyNames);
                double product;
                if (sigmoid) {
                    UnivariateFunction func = new SelfPlusOneRatioFunction();
                    product = func.value(entity.get(leftName).asDouble()) *
                            func.value(entity.get(rightName).asDouble());
                } else {
                    product = entity.get(leftName).asDouble() * entity.get(rightName).asDouble();
                }
                FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(feaList, update,
                        indexSpace, indexName, key, product);
                feaMap.put(feaName, feaList);
            }
        }
        return feaMap;
    }
}
