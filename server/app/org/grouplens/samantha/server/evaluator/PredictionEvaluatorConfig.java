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

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.common.AbstractComponentConfig;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.dao.EntityDAOUtilities;
import org.grouplens.samantha.modeler.metric.Metric;
import org.grouplens.samantha.server.evaluator.metric.MetricConfig;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.indexer.Indexer;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.Predictor;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class PredictionEvaluatorConfig extends AbstractComponentConfig implements EvaluatorConfig {
    final private Injector injector;
    final private String predictorName;
    final private String predictorNameKey;
    final private List<String> groupKeys;
    final private List<String> indexerNames;
    final private List<String> predIndexerNames;
    final private List<MetricConfig> metricConfigs;
    final private Configuration daoConfigs;
    final private String daoConfigKey;

    private PredictionEvaluatorConfig(Configuration config,
                                      List<MetricConfig> metricConfigs,
                                      String predictorName,
                                      String predictorNameKey,
                                      List<String> groupKeys,
                                      List<String> indexerNames,
                                      List<String> predIndexerNames,
                                      Configuration daoConfigs,
                                      Injector injector, String daoConfigKey) {
        super(config);
        this.metricConfigs = metricConfigs;
        this.predictorName = predictorName;
        this.predictorNameKey = predictorNameKey;
        this.groupKeys = groupKeys;
        this.indexerNames = indexerNames;
        this.predIndexerNames = predIndexerNames;
        this.injector = injector;
        this.daoConfigs = daoConfigs;
        this.daoConfigKey = daoConfigKey;
    }

    public static EvaluatorConfig getEvaluatorConfig(Configuration evalConfig,
                                              Injector injector) {
        List<MetricConfig> metricConfigs = EvaluatorUtilities
                .getMetricConfigs(evalConfig.getConfigList("metrics"), injector);
        return new PredictionEvaluatorConfig(evalConfig,
                metricConfigs,
                evalConfig.getString("predictor"),
                evalConfig.getString("predictorKey"),
                evalConfig.getStringList("groupKeys"),
                evalConfig.getStringList("indexers"),
                evalConfig.getStringList("predictionIndexers"),
                evalConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get()), injector,
                evalConfig.getString("daoConfigKey"));
    }

    public Evaluator getEvaluator(RequestContext requestContext) {
        JsonNode reqBody = requestContext.getRequestBody();
        SamanthaConfigService configService =
                injector.instanceOf(SamanthaConfigService.class);
        String predName = JsonHelpers.getOptionalString(reqBody, predictorNameKey, predictorName);
        Predictor predictor = configService.getPredictor(predName,
                requestContext);
        List<Metric> metrics = new ArrayList<>(metricConfigs.size());
        for (MetricConfig metricConfig : metricConfigs) {
            metrics.add(metricConfig.getMetric(requestContext));
        }
        List<Indexer> indexers = new ArrayList<>(indexerNames.size());
        for (String indexerName : indexerNames) {
            indexers.add(configService.getIndexer(indexerName, requestContext));
        }
        List<Indexer> predIndexers = new ArrayList<>(predIndexerNames.size());
        for (String indexerName : predIndexerNames) {
            predIndexers.add(configService.getIndexer(indexerName, requestContext));
        }
        EntityDAO entityDao = EntityDAOUtilities.getEntityDAO(daoConfigs, requestContext,
                reqBody.get(daoConfigKey), injector);
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        return new PredictionEvaluator(predictor, entityDao, entityExpanders,
                groupKeys, metrics, indexers, predIndexers);
    }
}
