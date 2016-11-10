package org.grouplens.samantha.server.dao;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public interface EntityDAOConfig {
    static EntityDAOConfig getEntityDAOConfig(Configuration daoConfig,
                                              Injector injector) {return null;}
    EntityDAO getEntityDAO(RequestContext requestContext, JsonNode reqDao);
}
