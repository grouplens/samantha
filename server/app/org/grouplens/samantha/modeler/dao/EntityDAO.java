package org.grouplens.samantha.modeler.dao;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface EntityDAO {
    boolean hasNextEntity();
    ObjectNode getNextEntity();
    void restart();
    void close();
}
