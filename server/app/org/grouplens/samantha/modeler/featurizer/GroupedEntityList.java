package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;

import java.util.ArrayList;
import java.util.List;

public class GroupedEntityList {
    private ObjectNode prevEntity;
    private String prevGroup;
    private final List<String> groupKeys;
    private final EntityDAO entityDAO;

    public GroupedEntityList(List<String> groupKeys, EntityDAO entityDAO) {
        this.entityDAO = entityDAO;
        this.groupKeys = groupKeys;
    }

    synchronized public List<ObjectNode> getNextGroup() {
        List<ObjectNode> entityList = new ArrayList<>();
        if (prevEntity != null) {
            entityList.add(prevEntity);
            prevEntity = null;
        }
        while (entityDAO.hasNextEntity()) {
            ObjectNode entity = entityDAO.getNextEntity();
            String group = FeatureExtractorUtilities.composeConcatenatedKey(entity, groupKeys);
            if (prevGroup == null) {
                prevGroup = group;
            }
            if (!group.equals(prevGroup)) {
                prevGroup = group;
                prevEntity = entity;
                break;
            } else {
                entityList.add(entity);
            }
        }
        return entityList;
    }

    synchronized public void restart() {
        entityDAO.restart();
        prevEntity = null;
        prevGroup = null;
    }

    synchronized public void close() {
        entityDAO.close();
        prevEntity = null;
        prevGroup = null;
    }
}
