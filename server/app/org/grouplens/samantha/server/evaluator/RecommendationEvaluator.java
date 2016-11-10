package org.grouplens.samantha.server.evaluator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.evaluator.metric.Metric;
import org.grouplens.samantha.server.indexer.Indexer;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.ranker.RankedResult;
import org.grouplens.samantha.server.recommender.Recommender;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import play.Logger;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

public class RecommendationEvaluator implements Evaluator {
    final private Recommender recommender;
    final private EntityDAO entityDAO;
    final private String type;
    //TODO: extend groupKey to a list of keys
    final private String groupKey;
    final private String recommenderName;
    final private String recommenderPara;
    final private List<Metric> metrics;
    final private List<Indexer> indexers;
    final private List<Indexer> recIndexers;

    public RecommendationEvaluator(String recommenderName,
                                   String recommenderPara,
                                   Recommender recommender,
                                   EntityDAO entityDAO,
                                   String type,
                                   String groupKey,
                                   List<Metric> metrics,
                                   List<Indexer> indexers, 
                                   List<Indexer> recIndexers) {
        this.recommender = recommender;
        this.entityDAO = entityDAO;
        this.metrics = metrics;
        this.indexers = indexers;
        this.recIndexers = recIndexers;
        this.type = type;
        this.groupKey = groupKey;
        this.recommenderName = recommenderName;
        this.recommenderPara = recommenderPara;
    }

    private void getRecommendationMetrics(String user, RequestContext requestContext,
                                          List<ObjectNode> entityList) {
        long start = System.currentTimeMillis();
        ObjectNode request = entityList.get(entityList.size() - 1).deepCopy();
        IOUtilities.parseEntityFromJsonNode(requestContext.getRequestBody(), request);
        request.put(groupKey, user);
        RequestContext context = new RequestContext(request, requestContext.getEngineName());
        RankedResult recommendations = recommender.recommend(context);
        for (Indexer indexer : recIndexers) {
            indexer.index(type, recommendations.toJson(), requestContext);
        }
        for (Metric metric : metrics) {
            metric.add(entityList, recommendations.getRankingList());
        }
        Logger.debug("Recommendation time: {}", System.currentTimeMillis() - start);
    }

    public List<ObjectNode> evaluate(RequestContext requestContext) {
        Logger.info("Note that the input evaluation data must be sorted by the group key, e.g. userId");
        List<ObjectNode> entityList = new ArrayList<>();
        String oldUser = null;
        int cnt = 0;
        while (entityDAO.hasNextEntity()) {
            ObjectNode entity = entityDAO.getNextEntity();
            String user = entity.get(groupKey).asText();
            if (oldUser == null) {
                oldUser = user;
            }
            if (!user.equals(oldUser)) {
                getRecommendationMetrics(oldUser, requestContext, entityList);
                cnt++;
                if (cnt % 1000 == 0) {
                    Logger.info("Evaluated on {} groups.", cnt);
                }
                entityList.clear();
                oldUser = user;
            }
            entityList.add(entity);
        }
        if (entityList.size() > 0) {
            getRecommendationMetrics(oldUser, requestContext, entityList);
        }
        return EvaluatorUtilities.indexMetrics(type, recommenderName, recommenderPara,
                requestContext, metrics, indexers);
    }
}
