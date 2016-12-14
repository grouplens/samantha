package org.grouplens.samantha.server.indexer;

import com.fasterxml.jackson.databind.JsonNode;

import org.elasticsearch.action.bulk.BulkResponse;
import org.grouplens.samantha.server.common.ElasticSearchService;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

//TODO: support remove and upsert options according to uniqueKeys
public class ESBasedIndexer extends AbstractIndexer {
    private final ElasticSearchService elasticSearchService;
    private final String elasticSearchIndex;
    private final String indexTypeKey;
    private final String indexType;
    private final String uniqueFieldsKey;
    private final List<String> uniqueFields;

    public ESBasedIndexer(ElasticSearchService elasticSearchService,
                          SamanthaConfigService configService,
                          Configuration daoConfigs,
                          String elasticSearchIndex,
                          String indexTypeKey,
                          String indexType,
                          String uniqueFieldsKey,
                          List<String> uniqueFields,
                          Injector injector, String daoConfigKey,
                          Configuration config) {
        super(config, configService, daoConfigs, daoConfigKey, injector);
        this.elasticSearchService = elasticSearchService;
        this.uniqueFieldsKey = uniqueFieldsKey;
        this.indexTypeKey = indexTypeKey;
        this.elasticSearchIndex = elasticSearchIndex;
        this.indexType = indexType;
        this.uniqueFields = uniqueFields;
    }

    private BulkResponse bulkIndex(String indexType, JsonNode data) {
        //TODO: first check whether existing based on config.uniqueFieldsKey if upsert
        return elasticSearchService.bulkIndex(elasticSearchIndex, indexType, data);
    }

    public void index(JsonNode data, RequestContext requestContext) {
        String indexType = JsonHelpers.getOptionalString(requestContext.getRequestBody(),
                indexTypeKey, this.indexType);
        BulkResponse resp = bulkIndex(indexType, data);
        if (resp.hasFailures()) {
            throw new BadRequestException(resp.buildFailureMessage());
        }
    }

}
