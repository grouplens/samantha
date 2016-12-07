package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.common.LearningInstance;

import java.util.ArrayList;
import java.util.List;

public class FeaturizerUtilities {
    private FeaturizerUtilities() {}

    static public List<LearningInstance> featurize(List<ObjectNode> entityList,
                                                   Featurizer featurizer, boolean update) {
        List<LearningInstance> instances = new ArrayList<>(entityList.size());
        for (ObjectNode entity : entityList) {
            LearningInstance ins = featurizer.featurize(entity, update);
            instances.add(ins);
        }
        return instances;
    }
}
