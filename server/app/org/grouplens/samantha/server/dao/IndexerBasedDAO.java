package org.grouplens.samantha.server.dao;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.indexer.Indexer;
import org.grouplens.samantha.server.io.RequestContext;

public class IndexerBasedDAO implements EntityDAO {
    private final EntityDAO entityDAO;

    public IndexerBasedDAO(Indexer indexer, RequestContext requestContext) {
        this.entityDAO = indexer.getEntityDAO(requestContext);
    }

    public boolean hasNextEntity() {
        return entityDAO.hasNextEntity();
    }

    public ObjectNode getNextEntity() {
        return entityDAO.getNextEntity();
    }

    public void restart() {
        entityDAO.restart();
    }

    public void close() {
        entityDAO.close();
    }
}
