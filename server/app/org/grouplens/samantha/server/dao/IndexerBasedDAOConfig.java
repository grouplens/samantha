package org.grouplens.samantha.server.dao;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class IndexerBasedDAOConfig implements EntityDAOConfig {
    private final String indexerName;
    private final String indexerKey;
    private final String indexerReqKey;
    private final Injector injector;

    private IndexerBasedDAOConfig(String indexerName, String indexerKey, Injector injector,
                                  String indexerReqKey) {
        this.indexerName = indexerName;
        this.indexerKey = indexerKey;
        this.injector = injector;
        this.indexerReqKey = indexerReqKey;
    }

    public static EntityDAOConfig getEntityDAOConfig(Configuration daoConfig,
                                                      Injector injector) {
        return new IndexerBasedDAOConfig(daoConfig.getString("indexerName"),
                daoConfig.getString("indexerNameKey"),
                injector, daoConfig.getString("indexerReqKey"));
    }
    public EntityDAO getEntityDAO(RequestContext requestContext, JsonNode reqDao) {
        String indexer = JsonHelpers.getOptionalString(reqDao, indexerKey, indexerName);
        JsonNode reqBody = JsonHelpers.getRequiredJson(reqDao, indexerReqKey);
        RequestContext pseudoReq = new RequestContext(reqBody, requestContext.getEngineName());
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        return new IndexerBasedDAO(configService.getIndexer(indexer, pseudoReq), pseudoReq);
    }
}
