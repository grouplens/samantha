package org.grouplens.samantha.server.dao;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;

import java.util.ArrayList;
import java.util.List;

public class ExpandedEntityDAO implements EntityDAO {
    private final List<EntityExpander> expanders;
    private final EntityDAO entityDAO;
    private final RequestContext requestContext;
    private List<ObjectNode> entityList = new ArrayList<>();
    private int idx = 0;

    public ExpandedEntityDAO(List<EntityExpander> expanders, EntityDAO entityDAO, RequestContext requestContext) {
        this.expanders = expanders;
        this.entityDAO = entityDAO;
        this.requestContext = requestContext;
    }

    public boolean hasNextEntity() {
        if (idx >= entityList.size()) {
            idx = 0;
            entityList = ExpanderUtilities.expandFromEntityDAO(entityDAO, entityList, expanders, requestContext);
            if (idx < entityList.size()) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public ObjectNode getNextEntity() {
        if (idx < entityList.size()) {
            return entityList.get(idx++);
        } else {
            idx = 0;
            entityList = ExpanderUtilities.expandFromEntityDAO(entityDAO, entityList, expanders, requestContext);
            if (entityList.size() > 0) {
                return entityList.get(idx++);
            }
        }
        return null;
    }

    public void restart() {
        entityDAO.restart();
        entityList.clear();
        idx = 0;
    }

    public void close() {
        entityDAO.close();
        entityList.clear();
        idx = 0;
    }
}
