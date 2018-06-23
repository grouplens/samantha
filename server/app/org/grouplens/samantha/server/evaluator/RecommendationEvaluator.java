/*
 * Copyright (c) [2016-2018] [University of Minnesota]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.grouplens.samantha.server.evaluator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.instance.GroupedEntityList;
import org.grouplens.samantha.modeler.metric.Metric;
import org.grouplens.samantha.modeler.metric.MetricResult;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
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
    final private List<EntityExpander> expanders;
    final private List<String> groupKeys;
    final private List<String> itemKeys;
    final private List<Metric> metrics;
    final private List<Indexer> indexers;
    final private List<Indexer> recIndexers;

    public RecommendationEvaluator(Recommender recommender,
                                   EntityDAO entityDAO,
                                   List<EntityExpander> expanders,
                                   List<String> groupKeys,
                                   List<String> itemKeys,
                                   List<Metric> metrics,
                                   List<Indexer> indexers, 
                                   List<Indexer> recIndexers) {
        this.recommender = recommender;
        this.entityDAO = entityDAO;
        this.expanders = expanders;
        this.metrics = metrics;
        this.indexers = indexers;
        this.recIndexers = recIndexers;
        this.groupKeys = groupKeys;
        this.itemKeys = itemKeys;
    }

    private void getRecommendationMetrics(RequestContext requestContext,
                                          List<ObjectNode> entityList) {
        if (itemKeys != null && itemKeys.size() > 0) {
            boolean include = false;
            for (JsonNode entity : entityList) {
                for (String itemKey : itemKeys) {
                    if (!"".equals(entity.get(itemKey).asText())) {
                        include = true;
                    }
                }
            }
            if (!include) {
                return;
            }
        }
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
        GroupedEntityList groupedEntityList = new GroupedEntityList(groupKeys, null, entityDAO);
        List<ObjectNode> entityList;
        int cnt = 0;
        int skipped = 0;
        while ((entityList = groupedEntityList.getNextGroup()).size() > 0) {
            entityList = ExpanderUtilities.expand(entityList, expanders, requestContext);
            if (entityList.size() > 0) {
                getRecommendationMetrics(requestContext, entityList);
                cnt++;
                if (cnt % 10000 == 0) {
                    Logger.info("Evaluated on {} groups.", cnt);
                }
            } else {
                skipped++;
            }
        }
        Logger.info("Evaluated on {} groups.", cnt);
        Logger.info("Skipped {} groups to evaluate on because of empty ground truth.", skipped);
        List<MetricResult> metricResults = EvaluatorUtilities.indexMetrics(recommender.getConfig(),
                requestContext, metrics, indexers);
        return new Evaluation(metricResults);
    }
}
