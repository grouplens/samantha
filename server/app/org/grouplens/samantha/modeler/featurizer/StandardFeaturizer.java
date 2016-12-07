package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.JsonNode;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.space.IndexSpace;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StandardFeaturizer implements Featurizer, Serializable {
    protected final IndexSpace indexSpace;
    protected final List<FeatureExtractor> featureExtractors;
    protected final List<String> features;
    protected final List<String> groupKeys;
    protected final String labelName;
    protected final String weightName;

    public StandardFeaturizer(IndexSpace indexSpace, List<FeatureExtractor> featureExtractors,
                              List<String> features, List<String> groupKeys,
                              String labelName, String weightName) {
        this.indexSpace = indexSpace;
        this.featureExtractors = featureExtractors;
        this.groupKeys = groupKeys;
        this.features = features;
        this.labelName = labelName;
        this.weightName = weightName;
    }

    public LearningInstance featurize(JsonNode entity, boolean update) {
        Map<String, List<Feature>> feaMap = new HashMap<>();
        for (FeatureExtractor extractor : featureExtractors) {
            feaMap.putAll(extractor.extract(entity, update,
                    indexSpace));
        }
        Int2DoubleMap feas = new Int2DoubleOpenHashMap();
        for (String fea : features) {
            if (feaMap.containsKey(fea)) {
                for (Feature feature : feaMap.get(fea)) {
                    feas.put(feature.getIndex(), feature.getValue());
                }
            }
        }
        double label = StandardLearningInstance.defaultLabel;
        double weight = StandardLearningInstance.defaultWeight;
        if (entity.has(labelName)) {
            label = entity.get(labelName).asDouble();
        } else if (feaMap.containsKey(labelName)) {
            label = feaMap.get(labelName).get(0).getValue();
        }
        if (entity.has(weightName)) {
            weight = entity.get(weightName).asDouble();
        } else if (feaMap.containsKey(weightName)) {
            weight = feaMap.get(weightName).get(0).getValue();
        }
        String group = null;
        if (groupKeys != null && groupKeys.size() > 0) {
            group = FeatureExtractorUtilities.composeConcatenatedKey(entity, groupKeys);
        }
        return new StandardLearningInstance(feas, label, weight, group);
    }
}
