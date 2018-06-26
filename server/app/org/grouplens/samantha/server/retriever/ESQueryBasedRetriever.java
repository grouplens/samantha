/*
 * Copyright (c) [2016-2018] [University of Minnesota]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.JsonNode;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.grouplens.samantha.server.common.ElasticSearchService;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

public class ESQueryBasedRetriever extends AbstractRetriever {
    private String scrollId = null;
    private boolean scrollComplete = false;
    private final ElasticSearchService elasticSearchService;
    private final String elasticSearchReqKey;
    private final String setScrollKey;
    private final String elasticSearchIndex;
    private final String retrieveType;
    private final List<String> retrieveFields;
    private final String[] matchFields;
    private final boolean defaultMatchAll;
    private final String queryKey;
    private final String elasticSearchScoreName;

    public ESQueryBasedRetriever(ElasticSearchService elasticSearchService,
                                 String elasticSearchScoreName,
                                 String elasticSearchReqKey, boolean defaultMatchAll,
                                 String setScrollKey, String elasticSearchIndex, String retrieveType,
                                 List<String> retrieveFields, Configuration config,
                                 List<String> matchFields, String queryKey,
                                 RequestContext requestContext, Injector injector) {
        super(config, requestContext, injector);
        this.elasticSearchService = elasticSearchService;
        this.elasticSearchScoreName= elasticSearchScoreName;
        this.elasticSearchIndex = elasticSearchIndex;
        this.elasticSearchReqKey = elasticSearchReqKey;
        this.setScrollKey = setScrollKey;
        this.retrieveFields = retrieveFields;
        this.retrieveType = retrieveType;
        this.defaultMatchAll = defaultMatchAll;
        if (matchFields != null) {
            this.matchFields = matchFields.toArray(new String[matchFields.size()]);
        } else {
            this.matchFields = new String[0];
        }
        this.queryKey = queryKey;
    }

    public RetrievedResult retrieve(RequestContext requestContext) {
        final JsonNode requestBody = requestContext.getRequestBody();
        boolean setScroll = JsonHelpers.getOptionalBoolean(requestBody, setScrollKey, false);
        if (setScroll && scrollComplete) {
            return new RetrievedResult(new ArrayList<>(), 0);
        }
        JsonNode elasticSearchRequest;
        if (requestBody.has(elasticSearchReqKey)) {
            elasticSearchRequest = JsonHelpers.getRequiredJson(requestBody, elasticSearchReqKey);
        } else if (matchFields.length > 0 && requestBody.has(queryKey)) {
            String query = JsonHelpers.getRequiredString(requestBody, queryKey);
            elasticSearchRequest = Json.parse(QueryBuilders.multiMatchQuery(query, matchFields).toString());
        } else if (defaultMatchAll || setScroll) {
            elasticSearchRequest = Json.parse(QueryBuilders.matchAllQuery().toString());
        } else {
            return new RetrievedResult(new ArrayList<>(), 0);
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
        RetrievedResult initialResult = ESRetrieverUtilities.parse(elasticSearchScoreName, requestContext,
                searchResponse, expanders, retrieveFields);
        if (setScroll) {
            scrollId = searchResponse.getScrollId();
            if (initialResult.getEntityList().size() == 0) {
                elasticSearchService.resetScroll(scrollId);
                scrollComplete = true;
            }
        }
        return initialResult;
    }
}
