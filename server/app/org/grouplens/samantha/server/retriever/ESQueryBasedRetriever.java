package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.JsonNode;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.grouplens.samantha.server.common.ElasticSearchService;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.io.RequestContext;
import play.libs.Json;

import java.util.List;

public class ESQueryBasedRetriever implements Retriever {
    private final ElasticSearchService elasticSearchService;
    private final ESQueryBasedRetrieverConfig config;
    private String scrollId = null;
    private final List<EntityExpander> expanders;

    public ESQueryBasedRetriever(ElasticSearchService elasticSearchService,
                                 List<EntityExpander> expanders,
                                 ESQueryBasedRetrieverConfig config) {
        this.elasticSearchService = elasticSearchService;
        this.expanders = expanders;
        this.config = config;
    }

    public RetrievedResult retrieve(RequestContext requestContext) {
        final JsonNode requestBody = requestContext.getRequestBody();
        JsonNode elasticSearchRequest;
        if (requestBody.has(config.elasticSearchReqKey)) {
            elasticSearchRequest = JsonHelpers.getRequiredJson(requestBody, config.elasticSearchReqKey);
        } else if (config.defaultElasticSearchReq != null) {
            elasticSearchRequest = Json.parse(config.defaultElasticSearchReq);
        } else {
            elasticSearchRequest = Json.parse(QueryBuilders.matchAllQuery().toString());
        }
        boolean setScroll = JsonHelpers.getOptionalBoolean(requestBody, config.setScrollKey, false);
        if (scrollId != null) {
            setScroll = false;
        }
        Integer size = JsonHelpers.getOptionalInt(elasticSearchRequest, "size", null);
        Integer from = JsonHelpers.getOptionalInt(elasticSearchRequest, "from", null);
        SearchResponse searchResponse = elasticSearchService
                .search(config.elasticSearchIndex,
                        config.retrieveType,
                        elasticSearchRequest,
                        config.retrieveFields,
                        setScroll,
                        scrollId, size, from);
        if (setScroll) {
            scrollId = searchResponse.getScrollId();
        }
        RetrievedResult initialResult = ESRetrieverUtilities.parse(config, requestContext,
                searchResponse, expanders);
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

    public JsonNode getFootprint() {
        return Json.newObject();
    }
}
