package org.grouplens.samantha.server.ranker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import play.Configuration;

import java.util.ArrayList;
import java.util.List;

public class EntityFieldBasedRanker extends AbstractRanker {
    private final int offset;
    private int limit;
    private final int pageSize;
    private final String whetherOrderKey;
    private final String orderFieldKey;
    private final String ascendingKey;

    public EntityFieldBasedRanker(Configuration config, int offset, int limit, int pageSize,
                                  String whetherOrderKey, String orderFieldKey, String ascendingKey) {
        super(config);
        this.offset = offset;
        this.limit = limit;
        this.pageSize = pageSize;
        this.whetherOrderKey = whetherOrderKey;
        this.orderFieldKey = orderFieldKey;
        this.ascendingKey = ascendingKey;
    }

    public RankedResult rank(RetrievedResult retrievedResult,
                             RequestContext requestContext) {
        List<ObjectNode> entityList = retrievedResult.getEntityList();
        if (pageSize == 0 || limit > entityList.size()) {
            limit = entityList.size();
        }
        List<Prediction> scoredList = new ArrayList<>(entityList.size());
        for (ObjectNode entity : entityList) {
            scoredList.add(new Prediction(entity, null, 0.0));
        }
        List<Prediction> candidates;
        JsonNode requestBody = requestContext.getRequestBody();
        boolean whetherOrder = JsonHelpers.getOptionalBoolean(requestBody, whetherOrderKey, true);
        if (whetherOrder) {
            String orderField = JsonHelpers.getRequiredString(requestBody, orderFieldKey);
            Ordering<Prediction> ordering = RankerUtilities.scoredResultFieldOrdering(orderField);
            boolean ascending = JsonHelpers.getOptionalBoolean(requestBody, ascendingKey, false);
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
}
