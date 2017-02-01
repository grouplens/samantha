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
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.dao.EntityDAOUtilities;
import org.grouplens.samantha.server.evaluator.metric.Metric;
import org.grouplens.samantha.server.evaluator.metric.MetricConfig;
import org.grouplens.samantha.server.indexer.Indexer;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.recommender.Recommender;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class RecommendationEvaluatorConfig implements EvaluatorConfig {
    final private Injector injector;
    final private String recommenderName;
    final private String recommenderNameKey;
    final private List<String> indexerNames;
    final private List<String> recIndexerNames;
    final private List<MetricConfig> metricConfigs;
    final private Configuration daoConfigs;
    final private String daoConfigKey;
    final private List<String> groupKeys;

    private RecommendationEvaluatorConfig(List<MetricConfig> metricConfigs,
                                          String recommenderName,
                                          String recommenderNameKey,
                                          List<String> indexerNames,
                                          List<String> recIndexerNames,
                                          Configuration daoConfigs,
                                          List<String> groupKeys,
                                          String daoConfigKey,
                                          Injector injector) {
        this.metricConfigs = metricConfigs;
        this.recommenderName = recommenderName;
        this.recommenderNameKey = recommenderNameKey;
        this.indexerNames = indexerNames;
        this.recIndexerNames = recIndexerNames;
        this.injector = injector;
        this.daoConfigKey = daoConfigKey;
        this.groupKeys = groupKeys;
        this.daoConfigs = daoConfigs;
    }

    public static EvaluatorConfig getEvaluatorConfig(Configuration evalConfig,
                                                     Injector injector) {
        List<MetricConfig> metricConfigs = EvaluatorUtilities
                .getMetricConfigs(evalConfig.getConfigList("metrics"), injector);
        return new RecommendationEvaluatorConfig(metricConfigs,
                evalConfig.getString("recommender"),
                evalConfig.getString("recommenderKey"),
                evalConfig.getStringList("indexers"),
                evalConfig.getStringList("recommendationIndexers"),
                evalConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get()),
                evalConfig.getStringList("groupKeys"),
                evalConfig.getString("daoConfigKey"),
                injector);
    }

    public Evaluator getEvaluator(RequestContext requestContext) {
        JsonNode reqBody = requestContext.getRequestBody();
        SamanthaConfigService configService =
                injector.instanceOf(SamanthaConfigService.class);
        String recName = JsonHelpers.getOptionalString(reqBody, recommenderNameKey,
                recommenderName);
        Recommender recommender = configService.getRecommender(recName,
                requestContext);
        List<Metric> metrics = new ArrayList<>(metricConfigs.size());
        for (MetricConfig metricConfig : metricConfigs) {
            metrics.add(metricConfig.getMetric(requestContext));
        }
        List<Indexer> indexers = new ArrayList<>(indexerNames.size());
        for (String indexerName : indexerNames) {
            indexers.add(configService.getIndexer(indexerName, requestContext));
        }
        List<Indexer> recIndexers = new ArrayList<>(recIndexerNames.size());
        for (String indexerName : recIndexerNames) {
            recIndexers.add(configService.getIndexer(indexerName, requestContext));
        }
        EntityDAO entityDao = EntityDAOUtilities.getEntityDAO(daoConfigs, requestContext,
                reqBody.get(daoConfigKey), injector);
        return new RecommendationEvaluator(recommender, entityDao, groupKeys,
                metrics, indexers, recIndexers);
    }
}
