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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.index.query.QueryBuilders;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.IOUtilities;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.grouplens.samantha.server.io.RequestContext;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

public class ESRetrieverUtilities {

    private ESRetrieverUtilities() {}

    static public RetrievedResult parse(String elasticSearchScoreName, RequestContext requestContext,
                                        SearchResponse searchResponse, List<EntityExpander> expanders,
                                        List<String> retrieveFields) {
        List<ObjectNode> entityList = new ArrayList<>(searchResponse.getHits().getHits().length);
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            ObjectNode entity = Json.newObject();
            ExpanderUtilities.parseEntityFromSearchHit(retrieveFields, elasticSearchScoreName,
                    hit, entity);
            entityList.add(entity);
        }
        entityList = ExpanderUtilities.expand(entityList, expanders, requestContext);
        long totalHits = searchResponse.getHits().getTotalHits();
        return new RetrievedResult(entityList, totalHits);
    }

    static public RetrievedResult requestOrDefaultMatch(RequestContext requestContext, ESQueryBasedRetriever retriever,
                                                        String defaultMatch, String elasticSearchReqKey) {
        JsonNode reqBody = requestContext.getRequestBody();
        if (reqBody.has(elasticSearchReqKey)) {
            return retriever.retrieve(requestContext);
        } else {
            String value = reqBody.get(defaultMatch).asText();
            ObjectNode pseudoReq = Json.newObject();
            IOUtilities.parseEntityFromJsonNode(reqBody, pseudoReq);
            pseudoReq.set(elasticSearchReqKey, Json.parse(QueryBuilders.matchQuery(defaultMatch,
                    value).toString()));
            return retriever.retrieve(new RequestContext(pseudoReq, requestContext.getEngineName()));
        }
    }
}
