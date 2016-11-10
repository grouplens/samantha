package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.grouplens.samantha.modeler.space.IndexSpace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnglishTokenizeExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private final String indexName;
    private final String attrName;
    private final String feaName;
    private final String vocabularyName;
    private final Analyzer analyzer;

    public EnglishTokenizeExtractor(String indexName,
                                    String attrName,
                                    String feaName,
                                    String vocabularyName) {
        this.indexName = indexName;
        this.attrName = attrName;
        this.feaName = feaName;
        this.vocabularyName = vocabularyName;
        this.analyzer = new EnglishAnalyzer();
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        Map<String, List<Feature>> feaMap = new HashMap<>();
        if (entity.has(attrName)) {
            List<Feature> features = new ArrayList<>();
            String text = entity.get(attrName).asText();
            Map<String, Integer> termFreq = FeatureExtractorUtilities.getTermFreq(analyzer, text, attrName);
            for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
                String key = FeatureExtractorUtilities.composeKey(vocabularyName, entry.getKey());
                FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(features, update,
                        indexSpace, indexName, key, entry.getValue());
            }
            feaMap.put(feaName, features);
        }
        return feaMap;
    }
}
