package org.grouplens.samantha.server.evaluator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.featurizer.GroupedEntityList;
import org.grouplens.samantha.server.evaluator.metric.Metric;
import org.grouplens.samantha.server.evaluator.metric.MetricResult;
import org.grouplens.samantha.server.indexer.Indexer;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.ranker.RankedResult;
import org.grouplens.samantha.server.recommender.Recommender;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import play.Logger;

import java.util.List;

public class RecommendationEvaluator implements Evaluator {
    final private Recommender recommender;
    final private EntityDAO entityDAO;
    final private List<String> groupKeys;
    final private List<Metric> metrics;
    final private List<Indexer> indexers;
    final private List<Indexer> recIndexers;

    public RecommendationEvaluator(Recommender recommender,
                                   EntityDAO entityDAO,
                                   List<String> groupKeys,
                                   List<Metric> metrics,
                                   List<Indexer> indexers, 
                                   List<Indexer> recIndexers) {
        this.recommender = recommender;
        this.entityDAO = entityDAO;
        this.metrics = metrics;
        this.indexers = indexers;
        this.recIndexers = recIndexers;
        this.groupKeys = groupKeys;
    }

    private void getRecommendationMetrics(RequestContext requestContext,
                                          List<ObjectNode> entityList) {
        long start = System.currentTimeMillis();
        ObjectNode request = entityList.get(entityList.size() - 1).deepCopy();
        IOUtilities.parseEntityFromJsonNode(requestContext.getRequestBody(), request);
        RequestContext context = new RequestContext(request, requestContext.getEngineName());
        RankedResult recommendations = recommender.recommend(context);
        for (Indexer indexer : recIndexers) {
            indexer.index(recommendations.toJson(), requestContext);
        }
        for (Metric metric : metrics) {
            metric.add(entityList, recommendations.getRankingList());
        }
        Logger.debug("Recommendation time: {}", System.currentTimeMillis() - start);
    }

    public Evaluation evaluate(RequestContext requestContext) {
        Logger.info("Note that the input evaluation data must be sorted by the group keys, e.g. groupId");
        GroupedEntityList groupedEntityList = new GroupedEntityList(groupKeys, entityDAO);
        List<ObjectNode> entityList;
        int cnt = 0;
        while ((entityList = groupedEntityList.getNextGroup()).size() > 0) {
            getRecommendationMetrics(requestContext, entityList);
            cnt++;
            if (cnt % 10000 == 0) {
                Logger.info("Evaluated on {} groups.", cnt);
            }
        }
        List<MetricResult> metricResults = EvaluatorUtilities.indexMetrics(recommender.getConfig(),
                requestContext, metrics, indexers);
        return new Evaluation(metricResults);
    }
}
