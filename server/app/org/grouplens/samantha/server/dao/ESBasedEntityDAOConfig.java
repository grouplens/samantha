package org.grouplens.samantha.server.dao;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.retriever.ESQueryBasedRetriever;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import play.Configuration;
import play.inject.Injector;

public class ESBasedEntityDAOConfig implements EntityDAOConfig {
    final Injector injector;
    final String retrieverName;
    final String elasticSearchReqKey;

    private ESBasedEntityDAOConfig(Injector injector,
                                   String retrieverName,
                                   String elasticSearchReqKey) {
        this.injector = injector;
        this.retrieverName = retrieverName;
        this.elasticSearchReqKey = elasticSearchReqKey;
    }

    static public EntityDAOConfig getEntityDAOConfig(Configuration daoConfig,
                                              Injector injector) {
        return new ESBasedEntityDAOConfig(injector,
                daoConfig.getString("retriever"), daoConfig.getString("elasticSearchReqKey"));
    }

    public EntityDAO getEntityDAO(RequestContext requestContext, JsonNode daoConfig) {
        SamanthaConfigService configService = injector
                .instanceOf(SamanthaConfigService.class);
        return new ESBasedEntityDAO((ESQueryBasedRetriever)configService
                .getRetriever(retrieverName, requestContext), requestContext, this);
    }
}
