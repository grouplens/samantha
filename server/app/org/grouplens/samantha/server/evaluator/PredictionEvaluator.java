/*
 * Copyright (c) [2016-2017] [University of Minnesota]
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.instance.GroupedEntityList;
import org.grouplens.samantha.modeler.metric.MetricResult;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.modeler.metric.Metric;
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
    final private List<String> groupKeys;
    final private List<Metric> metrics;
    final private List<Indexer> indexers;
    final private List<Indexer> predIndexers;

    public PredictionEvaluator(Predictor predictor,
                               EntityDAO entityDAO,
                               List<String> groupKeys,
                               List<Metric> metrics,
                               List<Indexer> indexers,
                               List<Indexer> predIndexers) {
        this.predictor = predictor;
        this.entityDAO = entityDAO;
        this.metrics = metrics;
        this.indexers = indexers;
        this.groupKeys = groupKeys;
        this.predIndexers = predIndexers;
    }

    private void getPredictionMetrics(RequestContext requestContext, List<ObjectNode> entityList) {
        List<Prediction> predictions = predictor.predict(entityList,
                requestContext);
        List<ObjectNode> processed = new ArrayList<>();
        for (Prediction pred : predictions) {
            for (Indexer indexer : predIndexers) {
                indexer.index(pred.toJson(), requestContext);
            }
            processed.add(pred.getEntity());
        }
        for (Metric metric : metrics) {
            metric.add(processed, predictions);
        }
    }

    public Evaluation evaluate(RequestContext requestContext) {
        if (groupKeys != null && groupKeys.size() > 0) {
            Logger.info("Note that the input evaluation data must be sorted by the group keys, e.g. groupId");
        }
        GroupedEntityList groupedEntityList = new GroupedEntityList(groupKeys, 1, entityDAO);
        List<ObjectNode> entityList;
        int cnt = 0;
        while ((entityList = groupedEntityList.getNextGroup()).size() > 0) {
            getPredictionMetrics(requestContext, entityList);
            cnt++;
            if (cnt % 10000 == 0) {
                Logger.info("Evaluated on {} groups.", cnt);
            }
        }
        List<MetricResult> metricResults = EvaluatorUtilities.indexMetrics(predictor.getConfig(),
                requestContext, metrics, indexers);
        return new Evaluation(metricResults);
    }
}
