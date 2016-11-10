package org.grouplens.samantha.server.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class RequestEntityDAOConfig implements EntityDAOConfig {
    final Injector injector;
    final String entitiesKey;

    private RequestEntityDAOConfig(Injector injector, String entitiesKey) {
        this.injector = injector;
        this.entitiesKey = entitiesKey;
    }

    static public EntityDAOConfig getEntityDAOConfig(Configuration daoConfig,
                                                     Injector injector) {
        return new RequestEntityDAOConfig(injector, daoConfig.getString("entitiesKey"));
    }

    public EntityDAO getEntityDAO(RequestContext requestContext, JsonNode reqDao) {
        ArrayNode entities = JsonHelpers.getRequiredArray(reqDao, entitiesKey);
        return new RequestEntityDAO(entities);
    }
}
