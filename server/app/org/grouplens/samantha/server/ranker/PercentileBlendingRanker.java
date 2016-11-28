package org.grouplens.samantha.server.ranker;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.grouplens.samantha.modeler.tree.SortingUtilities;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import play.Configuration;

import java.util.ArrayList;
import java.util.List;

public class PercentileBlendingRanker extends AbstractRanker {
    private final List<EntityExpander> entityExpanders;
    private final Object2DoubleMap<String> defaults;
    private final int offset;
    private final int pageSize;
    private final int limit;

    public PercentileBlendingRanker(Object2DoubleMap<String> defaults, int offset, int limit, int pageSize,
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
        int listSize = entityList.size();
        if (listSize > 0) {
            for (Object2DoubleMap.Entry<String> entry : defaults.object2DoubleEntrySet()) {
                String key = entry.getKey();
                entityList.sort(SortingUtilities.jsonFieldComparator(key));
                for (int i = 0; i < entityList.size(); i++) {
                    entityList.get(i).put(key + "Percentile", (double) i / listSize);
                }
            }
        }
        int curLimit = limit;
        if (pageSize == 0 || limit > listSize) {
            curLimit = entityList.size();
        }
        List<Prediction> scoredList = new ArrayList<>(entityList.size());
        for (ObjectNode entity : entityList) {
            double score = 0.0;
            for (Object2DoubleMap.Entry<String> entry : defaults.object2DoubleEntrySet()) {
                String key = entry.getKey();
                score += (entry.getDoubleValue() * entity.get(key + "Percentile").asDouble());
            }
            scoredList.add(new Prediction(entity, null, score));
        }
        Ordering<Prediction> ordering = RankerUtilities.scoredResultScoreOrdering();
        List<Prediction> candidates = ordering
                .greatestOf(scoredList, offset + curLimit);
        List<Prediction> recs;
        if (candidates.size() < offset) {
            recs = new ArrayList<>();
        } else {
            recs = candidates.subList(offset, candidates.size());
        }
        return new RankedResult(recs, offset, curLimit, scoredList.size());
    }
}
