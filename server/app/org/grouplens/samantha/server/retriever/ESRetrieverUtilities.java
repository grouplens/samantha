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
