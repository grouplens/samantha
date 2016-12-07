package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.common.LearningInstance;

import java.util.ArrayList;
import java.util.List;

public class FeaturizerUtilities {
    private FeaturizerUtilities() {}

    static public List<LearningInstance> featurize(List<ObjectNode> entityList, List<String> groupKeys,
                                                   Featurizer featurizer, boolean update) {
        List<LearningInstance> instances = new ArrayList<>(entityList.size());
        String group = null;
        if (entityList.size() > 0 && groupKeys != null && groupKeys.size() > 1) {
            group = FeatureExtractorUtilities.composeConcatenatedKey(entityList.get(0), groupKeys);
        }
        for (ObjectNode entity : entityList) {
            LearningInstance ins = featurizer.featurize(entity, update);
            ins.setGroup(group);
            instances.add(ins);
        }
        return instances;
    }
}
