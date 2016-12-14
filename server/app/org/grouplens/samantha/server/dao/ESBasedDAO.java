package org.grouplens.samantha.server.dao;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.index.query.QueryBuilders;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.retriever.ESQueryBasedRetriever;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import play.libs.Json;

import java.util.List;

public class ESBasedDAO implements EntityDAO {

    private final ESQueryBasedRetriever retriever;
    private final String engineName;
    private int idx = 0;
    private List<ObjectNode> entityList = null;
    private final RequestContext requestContext;

    public ESBasedDAO(ESQueryBasedRetriever retriever,
                      RequestContext requestContext,
                      ESBasedDAOConfig config) {
        this.retriever = retriever;
        this.engineName = requestContext.getEngineName();
        ObjectNode req = Json.newObject();
        IOUtilities.parseEntityFromJsonNode(requestContext.getRequestBody(), req);
        req.set(config.elasticSearchReqKey, Json.parse(QueryBuilders.matchAllQuery().toString()));
        req.put("setScroll", true);
        this.requestContext = new RequestContext(req, engineName);
    }

    public ObjectNode getNextEntity() {
        if (entityList != null && idx < entityList.size()) {
            return entityList.get(idx++);
        } else {
            RetrievedResult retrievedResult = retriever.retrieve(requestContext);
            entityList = retrievedResult.getEntityList();
            idx = 0;
            if (idx < entityList.size()) {
                return entityList.get(idx);
            } else {
                return null;
            }
        }
    }

    public boolean hasNextEntity() {
        if (entityList == null || idx >= entityList.size()) {
            RetrievedResult retrievedResult = retriever.retrieve(requestContext);
            entityList = retrievedResult.getEntityList();
            idx = 0;
        }
        if (idx < entityList.size()) {
            return true;
        } else {
            return false;
        }
    }

    public void restart() {
        retriever.resetScroll();
        idx = 0;
        entityList = null;
    }

    public void close() {}
}
