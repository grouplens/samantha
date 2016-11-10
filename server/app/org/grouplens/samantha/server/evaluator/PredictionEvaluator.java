package org.grouplens.samantha.server.evaluator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.evaluator.metric.Metric;
import org.grouplens.samantha.server.indexer.Indexer;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.Predictor;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import play.Logger;

import java.util.ArrayList;
import java.util.List;

public class PredictionEvaluator implements Evaluator {
    final private Predictor predictor;
    final private EntityDAO entityDAO;
    final private String type;
    final private String predictorName;
    final private String predictorPara;
    final private String groupKey;
    final private List<Metric> metrics;
    final private List<Indexer> indexers;
    final private List<Indexer> predIndexers;

    public PredictionEvaluator(String predictorName,
                               String predictorPara,
                               Predictor predictor,
                               EntityDAO entityDAO,
                               String type,
                               String groupKey,
                               List<Metric> metrics,
                               List<Indexer> indexers,
                               List<Indexer> predIndexers) {
        this.predictor = predictor;
        this.entityDAO = entityDAO;
        this.metrics = metrics;
        this.indexers = indexers;
        this.type = type;
        this.groupKey = groupKey;
        this.predictorName = predictorName;
        this.predictorPara = predictorPara;
        this.predIndexers = predIndexers;
    }

    private void getPredictionMetrics(RequestContext requestContext, List<ObjectNode> entityList) {
        List<Prediction> predictions = predictor.predict(entityList,
                requestContext);
        List<ObjectNode> processed = new ArrayList<>();
        for (Prediction pred : predictions) {
            for (Indexer indexer : predIndexers) {
                indexer.index(type, pred.toJson(), requestContext);
            }
            processed.add(pred.getEntity());
        }
        for (Metric metric : metrics) {
            metric.add(processed, predictions);
        }
    }

    public List<ObjectNode> evaluate(RequestContext requestContext) {
        List<ObjectNode> entityList = new ArrayList<>();
        String oldUser = null;
        if (groupKey != null) {
            Logger.info("Note that the input evaluation data must be sorted by the group key, e.g. userId");
        }
        while (entityDAO.hasNextEntity()) {
            ObjectNode entity = entityDAO.getNextEntity();
            if (groupKey == null) {
                entityList.add(entity);
                getPredictionMetrics(requestContext, entityList);
                entityList.clear();
            } else {
                String user = entity.get(groupKey).asText();
                if (oldUser == null) {
                    oldUser = user;
                }
                if (!user.equals(oldUser)) {
                    getPredictionMetrics(requestContext, entityList);
                    entityList.clear();
                    oldUser = user;
                }
                entityList.add(entity);
            }
        }
        if (entityList.size() > 0) {
            getPredictionMetrics(requestContext, entityList);
        }
        return EvaluatorUtilities.indexMetrics(type, predictorName, predictorPara,
                requestContext, metrics, indexers);
    }
}
