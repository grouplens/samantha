package org.grouplens.samantha.server.predictor.learning;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.featurizer.GroupedEntityList;
import org.grouplens.samantha.server.exception.BadRequestException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class SyncDeserializedLearningData implements LearningData {
    private final EntityDAO entityDAO;
    private final GroupedEntityList groupedEntityList;
    private final String insAttr;
    private final String labelAttr;
    private final String weightAttr;

    //TODO: since learning instance now has group info, groupKeys is not necessary anymore
    public SyncDeserializedLearningData(EntityDAO entityDAO, String insAttr, List<String> groupKeys,
                                        String labelAttr, String weightAttr) {
        this.entityDAO = entityDAO;
        this.insAttr = insAttr;
        this.labelAttr = labelAttr;
        this.weightAttr = weightAttr;
        if (groupKeys != null && groupKeys.size() > 0) {
            groupedEntityList = new GroupedEntityList(groupKeys, entityDAO);
        } else {
            groupedEntityList = null;
        }
    }

    private LearningInstance deserializeLearningInstance(ObjectNode entity) {
        try {
            byte[] data = Base64.getDecoder().decode(entity.get(insAttr).asText());
            ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(data));
            LearningInstance instance = (LearningInstance) stream.readUnshared();
            stream.close();
            if (entity.has(labelAttr)) {
                instance.setLabel(entity.get(labelAttr).asDouble());
            }
            if (entity.has(weightAttr)) {
                instance.setWeight(entity.get(weightAttr).asDouble());
            }
            return instance;
        } catch (IOException | ClassNotFoundException e) {
            throw new BadRequestException(e);
        }
    }

    private List<LearningInstance> deserializeEntityList(List<ObjectNode> entityList) {
        List<LearningInstance> instances = new ArrayList<>(entityList.size());
        for (ObjectNode entity : entityList) {
            instances.add(deserializeLearningInstance(entity));
        }
        return instances;
    }

    public List<LearningInstance> getLearningInstance() {
        if (groupedEntityList == null) {
            LearningInstance instance;
            ObjectNode entity;
            synchronized (entityDAO) {
                if (entityDAO.hasNextEntity()) {
                    entity = entityDAO.getNextEntity();
                } else {
                    entityDAO.close();
                    return null;
                }
            }
            instance = deserializeLearningInstance(entity);
            List<LearningInstance> instances = new ArrayList<>(1);
            instances.add(instance);
            return instances;
        } else {
            List<ObjectNode> entityList = groupedEntityList.getNextGroup();
            return deserializeEntityList(entityList);
        }
    }

    synchronized public void startNewIteration() {
        if (groupedEntityList == null) {
            entityDAO.restart();
        } else {
            groupedEntityList.restart();
        }
    }
}
