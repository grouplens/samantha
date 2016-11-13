package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.dao.EntityDAOUtilities;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class RequestBasedRetriever extends AbstractRetriever {
    private final List<EntityExpander> expanders;
    private final Configuration entityDaoConfigs;
    private final Injector injector;
    private final String daoConfigKey;

    public RequestBasedRetriever(Configuration entityDaoConfigs, List<EntityExpander> expanders,
                                 Injector injector, String daoConfigKey, Configuration config) {
        super(config);
        this.expanders = expanders;
        this.entityDaoConfigs = entityDaoConfigs;
        this.injector = injector;
        this.daoConfigKey = daoConfigKey;
    }

    public RetrievedResult retrieve(RequestContext requestContext) {
        JsonNode reqBody = requestContext.getRequestBody();
        List<ObjectNode> hits = new ArrayList<>();
        if (reqBody.has(daoConfigKey)) {
            JsonNode reqDao = JsonHelpers.getRequiredJson(requestContext.getRequestBody(), daoConfigKey);
            EntityDAO entityDAO = EntityDAOUtilities.getEntityDAO(entityDaoConfigs, requestContext,
                    reqDao, injector);
            while (entityDAO.hasNextEntity()) {
                hits.add(entityDAO.getNextEntity());
            }
            hits = ExpanderUtilities.expand(hits, expanders, requestContext);
        }
        return new RetrievedResult(hits, hits.size());
    }
}
