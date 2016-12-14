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
import org.grouplens.samantha.server.predictor.Predictor;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class PredictionEvaluatorConfig implements EvaluatorConfig {
    final private Injector injector;
    final private String predictorNameKey;
    final private List<String> groupKeys;
    final private List<String> indexerNames;
    final private List<String> predIndexerNames;
    final private List<MetricConfig> metricConfigs;
    final private Configuration daoConfigs;
    final private String daoConfigKey;

    private PredictionEvaluatorConfig(List<MetricConfig> metricConfigs,
                                      String predictorNameKey,
                                      List<String> groupKeys,
                                      List<String> indexerNames,
                                      List<String> predIndexerNames,
                                      Configuration daoConfigs,
                                      Injector injector, String daoConfigKey) {
        this.metricConfigs = metricConfigs;
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
        return new PredictionEvaluatorConfig(metricConfigs,
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
        String predictorName = JsonHelpers.getRequiredString(reqBody, predictorNameKey);
        Predictor predictor = configService.getPredictor(predictorName,
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
        return new PredictionEvaluator(predictor, entityDao, groupKeys, metrics, indexers, predIndexers);
    }
}
