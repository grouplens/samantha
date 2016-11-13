package org.grouplens.samantha.server.ranker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import play.Configuration;

import java.util.ArrayList;
import java.util.List;

public class FieldBlendingRanker extends AbstractRanker {
    private final List<EntityExpander> entityExpanders;
    private final Object2DoubleMap<String> defaults;
    private final int offset;
    private final int pageSize;
    private int limit;

    public FieldBlendingRanker(Object2DoubleMap<String> defaults, int offset, int limit, int pageSize,
                               List<EntityExpander> entityExpanders, Configuration config) {
        super(config);
        this.defaults = defaults;
        this.offset = offset;
        this.limit = limit;
        this.pageSize = pageSize;
        this.entityExpanders = entityExpanders;
    }

    public RankedResult rank(RetrievedResult retrievedResult, RequestContext requestContext) {
        List<ObjectNode> entityList = retrievedResult.getEntityList();
        for (EntityExpander expander : entityExpanders) {
            entityList = expander.expand(entityList, requestContext);
        }
        if (pageSize == 0 || limit > entityList.size()) {
            limit = entityList.size();
        }
        Object2DoubleMap<String> field2min = new Object2DoubleOpenHashMap<>(defaults.size());
        Object2DoubleMap<String> field2max = new Object2DoubleOpenHashMap<>(defaults.size());
        for (Object2DoubleMap.Entry<String> entry : defaults.object2DoubleEntrySet()) {
            String key = entry.getKey();
            for (JsonNode entity : entityList) {
                if (entity.has(key)) {
                    double curVal = entity.get(key).asDouble();
                    double curMin = field2min.getOrDefault(key, Double.MAX_VALUE);
                    double curMax = field2max.getOrDefault(key, Double.MIN_VALUE);
                    if (curMin > curVal) {
                        field2min.put(key, curVal);
                    }
                    if (curMax < curVal) {
                        field2max.put(key, curVal);
                    }
                }
            }
        }
        List<Prediction> scoredList = new ArrayList<>(entityList.size());
        for (ObjectNode entity : entityList) {
            double score = 0.0;
            for (Object2DoubleMap.Entry<String> entry : defaults.object2DoubleEntrySet()) {
                String key = entry.getKey();
                if (entity.has(key)) {
                    double min = field2min.getDouble(key);
                    double max = field2max.getDouble(key);
                    double val = (min == max) ? 0.0 : (entity.get(key).asDouble() - min) / (max - min);
                    score += (entry.getDoubleValue() * val);
                }
            }
            scoredList.add(new Prediction(entity, null, score));
        }
        Ordering<Prediction> ordering = RankerUtilities.scoredResultScoreOrdering();
        List<Prediction> candidates = ordering
                .greatestOf(scoredList, offset + limit);
        List<Prediction> recs;
        if (candidates.size() < offset) {
            recs = new ArrayList<>();
        } else {
            recs = candidates.subList(offset, candidates.size());
        }
        return new RankedResult(recs, offset, limit, scoredList.size());
    }
}
