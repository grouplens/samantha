package org.grouplens.samantha.server.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.io.IOUtilities;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

public class RequestEntityDAO implements EntityDAO {
    final private List<ObjectNode> entityList;
    private int idx = 0;

    public RequestEntityDAO(ArrayNode entities) {
        entityList = new ArrayList<>(entities.size());
        for (JsonNode json : entities) {
            ObjectNode entity = Json.newObject();
            IOUtilities.parseEntityFromJsonNode(json, entity);
            entityList.add(entity);
        }
    }

    public boolean hasNextEntity() {
        return idx < entityList.size();
    }

    public ObjectNode getNextEntity() {
        return entityList.get(idx++);
    }

    public void restart() {
        idx = 0;
    }

    public void close() {}
}
