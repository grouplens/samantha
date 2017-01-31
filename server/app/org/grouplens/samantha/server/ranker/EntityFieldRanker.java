package org.grouplens.samantha.server.ranker;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import play.Configuration;

import java.util.ArrayList;
import java.util.List;

//TODO: support multiple fields, partial sorting with multiple fields
public class EntityFieldRanker extends AbstractRanker {
    private final int offset;
    private final int limit;
    private final int pageSize;
    private final boolean whetherOrder;
    private final String orderField;
    private final boolean ascending;

    public EntityFieldRanker(Configuration config, int offset, int limit, int pageSize,
                             boolean whetherOrder, String orderField, boolean ascending) {
        super(config);
        this.offset = offset;
        this.limit = limit;
        this.pageSize = pageSize;
        this.whetherOrder = whetherOrder;
        this.orderField = orderField;
        this.ascending = ascending;
    }

    public RankedResult rank(RetrievedResult retrievedResult,
                             RequestContext requestContext) {
        List<ObjectNode> entityList = retrievedResult.getEntityList();
        int curLimit = limit;
        if (pageSize == 0 || limit > entityList.size()) {
            curLimit = entityList.size();
        }
        List<Prediction> scoredList = new ArrayList<>(entityList.size());
        for (ObjectNode entity : entityList) {
            scoredList.add(new Prediction(entity, null, 0.0));
        }
        List<Prediction> candidates;
        if (whetherOrder) {
            Ordering<Prediction> ordering = RankerUtilities.scoredResultFieldOrdering(orderField);
            if (ascending) {
                candidates = ordering.leastOf(scoredList, offset + curLimit);
            } else {
                candidates = ordering.greatestOf(scoredList, offset + curLimit);
            }
        } else {
            candidates = scoredList;
        }
        List<Prediction> recs;
        if (offset >= candidates.size()) {
            recs = new ArrayList<>();
        } else {
            recs = candidates.subList(offset, curLimit);
        }
        return new RankedResult(recs, offset, curLimit, retrievedResult.getMaxHits());
    }
}
