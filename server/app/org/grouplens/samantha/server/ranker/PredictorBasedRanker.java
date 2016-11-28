package org.grouplens.samantha.server.ranker;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.Predictor;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import play.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PredictorBasedRanker extends AbstractRanker {
    private final List<EntityExpander> entityExpanders;
    private final Predictor predictor;
    private final int pageSize;
    private final int offset;
    private final int limit;

    public PredictorBasedRanker(Predictor predictor, int pageSize, int offset, int limit,
                                List<EntityExpander> entityExpanders, Configuration config) {
        super(config);
        this.predictor = predictor;
        this.pageSize = pageSize;
        this.offset = offset;
        this.limit = limit;
        this.entityExpanders = entityExpanders;
    }

    public RankedResult rank(RetrievedResult retrievedResult,
                             RequestContext requestContext) {
        List<ObjectNode> entityList = retrievedResult.getEntityList();
        List<Prediction> predictions = predictor.predict(entityList, requestContext);
        int curLimit = limit;
        if (pageSize == 0 || limit > predictions.size()) {
            curLimit = predictions.size();
        }
        Ordering<Prediction> ordering = RankerUtilities.scoredResultScoreOrdering();
        List<Prediction> candidates = ordering
                .greatestOf(predictions, offset + curLimit);
        List<Prediction> recs;
        if (candidates.size() < offset) {
            recs = new ArrayList<>();
        } else {
            recs = candidates.subList(offset, candidates.size());
        }
        List<ObjectNode> recEntities = new ArrayList<>(recs.size());
        for (Prediction pred : recs) {
            recEntities.add(pred.getEntity());
        }
        for (EntityExpander expander : entityExpanders) {
            expander.expand(recEntities, requestContext);
        }
        return new RankedResult(recs, offset, curLimit, predictions.size());
    }

    public Configuration getConfig() {
        Map<String, Object> configMap = config.asMap();
        configMap.put("predictorConfig", predictor.getConfig().asMap());
        return new Configuration(configMap);
    }
}
