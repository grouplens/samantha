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
    final private String recommenderNameKey;
    final private List<String> indexerNames;
    final private List<String> recIndexerNames;
    final private List<MetricConfig> metricConfigs;
    final private Configuration daoConfigs;
    final private String daoConfigKey;
    final private List<String> groupKeys;

    private RecommendationEvaluatorConfig(List<MetricConfig> metricConfigs,
                                          String recommenderNameKey,
                                          List<String> indexerNames,
                                          List<String> recIndexerNames,
                                          Configuration daoConfigs,
                                          List<String> groupKeys,
                                          String daoConfigKey,
                                          Injector injector) {
        this.metricConfigs = metricConfigs;
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
        String recommenderName = JsonHelpers.getRequiredString(reqBody, recommenderNameKey);
        Recommender recommender = configService.getRecommender(recommenderName,
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
