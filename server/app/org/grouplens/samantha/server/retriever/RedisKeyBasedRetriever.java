package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.common.RedisService;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

//TODO: support retrieve all from the indexPrefix without giving keys as the parameters
public class RedisKeyBasedRetriever implements Retriever {
    private final RedisService redisService;
    private final List<String> retrieveFields;
    private final String indexPrefix;
    private final List<String> keyFields;

    public RedisKeyBasedRetriever(RedisService redisService, List<String> retrieveFields,
                                  String indexPrefix, List<String> keyFields) {
        this.keyFields = keyFields;
        this.redisService = redisService;
        this.retrieveFields = retrieveFields;
        this.indexPrefix = indexPrefix;
    }

    private List<ObjectNode> retrieve(JsonNode data) {
        List<JsonNode> results = redisService.bulkGetFromHashSet(indexPrefix, keyFields, data);
        List<ObjectNode> list = new ArrayList<>(results.size());
        for (JsonNode result : results) {
            ObjectNode entity = Json.newObject();
            IOUtilities.parseEntityFromJsonNode(retrieveFields, result, entity);
            list.add(entity);
        }
        return list;
    }

    public RetrievedResult retrieve(RequestContext requestContext) {
        JsonNode reqBody = requestContext.getRequestBody();
        List<ObjectNode> hits = retrieve(reqBody);
        return new RetrievedResult(hits, hits.size());
    }

    public JsonNode getFootprint() {
        return Json.newObject();
    }
}
