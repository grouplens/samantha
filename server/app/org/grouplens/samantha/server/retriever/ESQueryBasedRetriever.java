package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.JsonNode;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.grouplens.samantha.server.common.ElasticSearchService;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.libs.Json;

import java.util.List;

public class ESQueryBasedRetriever extends AbstractRetriever {
    private final ElasticSearchService elasticSearchService;
    private String scrollId = null;
    private final List<EntityExpander> expanders;
    private final String elasticSearchReqKey;
    private final String defaultElasticSearchReq;
    private final String setScrollKey;
    private final String elasticSearchIndex;
    private final String retrieveType;
    private final List<String> retrieveFields;
    private final String elasticSearchScoreName;

    public ESQueryBasedRetriever(ElasticSearchService elasticSearchService,
                                 List<EntityExpander> expanders, String elasticSearchScoreName,
                                 String elasticSearchReqKey, String defaultElasticSearchReq,
                                 String setScrollKey, String elasticSearchIndex, String retrieveType,
                                 List<String> retrieveFields, Configuration config) {
        super(config);
        this.elasticSearchService = elasticSearchService;
        this.expanders = expanders;
        this.elasticSearchScoreName= elasticSearchScoreName;
        this.elasticSearchIndex = elasticSearchIndex;
        this.defaultElasticSearchReq = defaultElasticSearchReq;
        this.elasticSearchReqKey = elasticSearchReqKey;
        this.setScrollKey = setScrollKey;
        this.retrieveFields = retrieveFields;
        this.retrieveType = retrieveType;
    }

    public RetrievedResult retrieve(RequestContext requestContext) {
        final JsonNode requestBody = requestContext.getRequestBody();
        JsonNode elasticSearchRequest;
        if (requestBody.has(elasticSearchReqKey)) {
            elasticSearchRequest = JsonHelpers.getRequiredJson(requestBody, elasticSearchReqKey);
        } else if (defaultElasticSearchReq != null) {
            //TODO: provide the capability of parameterize this defaultElasticSearchReq with request body
            elasticSearchRequest = Json.parse(defaultElasticSearchReq);
        } else {
            elasticSearchRequest = Json.parse(QueryBuilders.matchAllQuery().toString());
        }
        boolean setScroll = JsonHelpers.getOptionalBoolean(requestBody, setScrollKey, false);
        if (scrollId != null) {
            setScroll = false;
        }
        Integer size = JsonHelpers.getOptionalInt(elasticSearchRequest, "size", null);
        Integer from = JsonHelpers.getOptionalInt(elasticSearchRequest, "from", null);
        SearchResponse searchResponse = elasticSearchService
                .search(elasticSearchIndex,
                        retrieveType,
                        elasticSearchRequest,
                        retrieveFields,
                        setScroll,
                        scrollId, size, from);
        if (setScroll) {
            scrollId = searchResponse.getScrollId();
        }
        RetrievedResult initialResult = ESRetrieverUtilities.parse(elasticSearchScoreName, requestContext,
                searchResponse, expanders, retrieveFields);
        return initialResult;
    }

    public String getScrollId() {
        return scrollId;
    }

    public void resetScroll() {
        if (scrollId != null) {
            elasticSearchService.resetScroll(scrollId);
        }
        scrollId = null;
    }
}
