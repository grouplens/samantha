package org.grouplens.samantha.server.indexer;

import com.fasterxml.jackson.databind.JsonNode;

import org.elasticsearch.action.bulk.BulkResponse;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.common.ElasticSearchService;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.dao.EntityDAOUtilities;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.RequestContext;

//TODO: support remove and upsert options according to uniqueKeys
public class ESBasedIndexer implements Indexer {
    private final ESBasedIndexerConfig config;
    private final ElasticSearchService elasticSearchService;

    public ESBasedIndexer(ElasticSearchService elasticSearchService,
                          ESBasedIndexerConfig config) {
        this.config = config;
        this.elasticSearchService = elasticSearchService;
    }

    private BulkResponse bulkIndex(String indexType, JsonNode data) {
        //TODO: first check whether existing based on config.uniqueFieldsKey if upsert
        return elasticSearchService.bulkIndex(config.elasticSearchIndex, indexType, data);
    }

    public void index(String type, JsonNode data, RequestContext requestContext) {
        bulkIndex(type, data);
    }

    public void index(RequestContext requestContext) {
        JsonNode reqBody = requestContext.getRequestBody();
        String indexType = JsonHelpers.getRequiredString(reqBody, config.indexTypeKey);
        EntityDAO entityDAO = EntityDAOUtilities.getEntityDAO(config.daoConfigs, requestContext,
                reqBody.get(config.daoConfigKey), config.injector);
        while (entityDAO.hasNextEntity()) {
            JsonNode documents = entityDAO.getNextEntity();
            BulkResponse resp = bulkIndex(indexType, documents);
            if (resp.hasFailures()) {
                throw new BadRequestException(resp.buildFailureMessage());
            }
        }
    }
}
