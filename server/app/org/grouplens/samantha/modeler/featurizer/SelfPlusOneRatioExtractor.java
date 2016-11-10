package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.math3.analysis.function.Log10;
import org.grouplens.samantha.modeler.space.IndexSpace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.abs;

public class SelfPlusOneRatioExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private final String indexName;
    private final String attrName;
    private final String feaName;
    private final boolean sparse;
    private final boolean log;

    public SelfPlusOneRatioExtractor(String indexName, String attrName, String feaName,
                                     boolean sparse, boolean log) {
        this.attrName = attrName;
        this.feaName = feaName;
        this.indexName = indexName;
        this.sparse = sparse;
        this.log = log;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        Map<String, List<Feature>> feaMap = new HashMap<>();
        if (entity.has(attrName)) {
            SelfPlusOneRatioFunction ratio = new SelfPlusOneRatioFunction();
            List<Feature> feaList = new ArrayList<>();
            double val = entity.get(attrName).asDouble();
            if (!sparse || val != 0.0) {
                if (log) {
                    Log10 log10 = new Log10();
                    val = log10.value(val + 1.0);
                }
                FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(feaList, update,
                        indexSpace, indexName, attrName, ratio.value(val));
                feaMap.put(feaName, feaList);
            }
        }
        return feaMap;
    }
}
