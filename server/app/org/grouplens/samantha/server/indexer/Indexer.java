package org.grouplens.samantha.server.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;

public interface Indexer {
    void index(RequestContext requestContext);
    void index(JsonNode data, RequestContext requestContext);
    ObjectNode getIndexedDataDAOConfig(RequestContext requestContext);
    EntityDAO getEntityDAO(RequestContext requestContext);
    void notifyDataSubscribers(RequestContext requestContext);
    Configuration getConfig();
}
