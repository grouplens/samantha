package org.grouplens.samantha.modeler.dao;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonFileEntityDAO implements EntityDAO {
    public boolean hasNextEntity() {
        return false;
    }
    public ObjectNode getNextEntity() {
        return null;
    }
    public void restart() {}
    public void close() {}
}
