package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;

import java.util.ArrayList;
import java.util.List;

public class SyncFeaturizedLearningData implements LearningData {
    private final GroupedEntityList groupedEntityList;
    private final EntityDAO entityDAO;
    private final Featurizer featurizer;
    private final boolean update;

    public SyncFeaturizedLearningData(EntityDAO entityDAO,
                                      List<String> groupKeys,
                                      Featurizer featurizer,
                                      boolean update) {
        this.entityDAO = entityDAO;
        this.featurizer = featurizer;
        this.update = update;
        if (groupKeys != null && groupKeys.size() > 0) {
            this.groupedEntityList = new GroupedEntityList(groupKeys, entityDAO);
        } else {
            this.groupedEntityList = null;
        }
    }


    public List<LearningInstance> getLearningInstance() {
        List<LearningInstance> instances;
        if (groupedEntityList != null) {
            List<ObjectNode> entityList;
            synchronized (groupedEntityList) {
                entityList = groupedEntityList.getNextGroup();
            }
            instances = FeaturizerUtilities.featurize(entityList, featurizer, update);
            entityList.clear();
            return instances;
        } else {
            ObjectNode cur = null;
            synchronized (entityDAO) {
                if (entityDAO.hasNextEntity()) {
                    cur = entityDAO.getNextEntity();
                } else {
                    entityDAO.close();
                }
            }
            instances = new ArrayList<>(1);
            if (cur != null) {
                instances.add(featurizer.featurize(cur, update));
            }
            return instances;
        }
    }

    synchronized public void startNewIteration() {
        if (groupedEntityList != null) {
            groupedEntityList.restart();
        } else {
            entityDAO.restart();
        }
    }
}
