package org.grouplens.samantha.server.ranker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

public class EntityFieldBasedRanker implements Ranker {
    private final EntityFieldBasedRankerConfig config;
    private final int offset;
    private int limit;

    public EntityFieldBasedRanker(EntityFieldBasedRankerConfig config, int offset, int limit) {
        this.config = config;
        this.offset = offset;
        this.limit = limit;
    }

    public RankedResult rank(RetrievedResult retrievedResult,
                             RequestContext requestContext) {
        List<ObjectNode> entityList = retrievedResult.getEntityList();
        if (config.pageSize == 0 || limit > entityList.size()) {
            limit = entityList.size();
        }
        List<Prediction> scoredList = new ArrayList<>(entityList.size());
        for (ObjectNode entity : entityList) {
            scoredList.add(new Prediction(entity, null, 0.0));
        }
        List<Prediction> candidates;
        JsonNode requestBody = requestContext.getRequestBody();
        boolean whetherOrder = JsonHelpers.getOptionalBoolean(requestBody,
                config.whetherOrderKey, true);
        if (whetherOrder) {
            String orderField = JsonHelpers.getRequiredString(requestBody,
                    config.orderFieldKey);
            Ordering<Prediction> ordering = RankerUtilities.scoredResultFieldOrdering(orderField);
            boolean ascending = JsonHelpers.getOptionalBoolean(requestBody, config.ascendingKey, false);
            if (ascending) {
                candidates = ordering.leastOf(scoredList, offset + limit);
            } else {
                candidates = ordering.greatestOf(scoredList, offset + limit);
            }
        } else {
            candidates = scoredList;
        }
        List<Prediction> recs;
        if (offset >= candidates.size()) {
            recs = new ArrayList<>();
        } else {
            recs = candidates.subList(offset, limit);
        }
        return new RankedResult(recs, offset, limit, retrievedResult.getMaxHits());
    }

    public JsonNode getFootprint() {
        return Json.newObject();
    }
}
