package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.tree.DecisionTree;
import org.grouplens.samantha.server.io.RequestContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DecisionTreeLeafExtractor implements FeatureExtractor {
    private final DecisionTree decisionTree;
    private final String feaName;
    private final String indexName;
    private final RequestContext requestContext;

    public DecisionTreeLeafExtractor(DecisionTree decisionTree, String feaName, String indexName,
                                     RequestContext requestContext) {
        this.decisionTree = decisionTree;
        this.feaName = feaName;
        this.indexName = indexName;
        this.requestContext = requestContext;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        int leafIdx = decisionTree.predictLeaf(entity);
        List<Feature> feaList = new ArrayList<>(1);
        String key = FeatureExtractorUtilities.composeKey(feaName, Integer.valueOf(leafIdx).toString());
        FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(feaList, update,
                indexSpace, indexName, key, 1.0);
        Map<String, List<Feature>> feaMap = new HashMap<>();
        feaMap.put(feaName, feaList);
        return feaMap;
    }
}
