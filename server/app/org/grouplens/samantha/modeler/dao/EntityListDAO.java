package org.grouplens.samantha.modeler.dao;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public class EntityListDAO implements EntityDAO {
    private int iter = 0;
    private final List<ObjectNode> entityList;

    public EntityListDAO(List<ObjectNode> entityList) {
        this.entityList = entityList;
    }

    public boolean hasNextEntity() {
        if (iter >= entityList.size()) {
            return false;
        } else {
            return true;
        }
    }

    public ObjectNode getNextEntity() {
        return entityList.get(iter++);
    }

    public void restart() {
        iter = 0;
    }

    public void close() {}
}
