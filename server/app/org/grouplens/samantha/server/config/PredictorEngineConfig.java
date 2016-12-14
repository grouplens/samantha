package org.grouplens.samantha.server.config;

import org.grouplens.samantha.server.evaluator.EvaluatorConfig;
import org.grouplens.samantha.server.exception.ConfigurationException;
import org.grouplens.samantha.server.indexer.IndexerConfig;
import org.grouplens.samantha.server.predictor.PredictorConfig;
import org.grouplens.samantha.server.ranker.RankerConfig;
import org.grouplens.samantha.server.recommender.RecommenderConfig;
import org.grouplens.samantha.server.retriever.RetrieverConfig;
import org.grouplens.samantha.server.router.RouterConfig;
import org.grouplens.samantha.server.scheduler.SchedulerConfig;

import java.util.Map;

class PredictorEngineConfig implements EngineConfig {
    final private Map<String, RetrieverConfig> retrieverConfigs;
    final private Map<String, PredictorConfig> predictorConfigs;
    private final Map<String, IndexerConfig> indexerConfigs;
    private final Map<String, EvaluatorConfig> evaluatorConfigs;
    private final Map<String, SchedulerConfig> schedulerConfigs;
    final private RouterConfig routerConfig;

    PredictorEngineConfig(Map<String, RetrieverConfig> retrieverConfigs,
                          Map<String, PredictorConfig> predictorConfigs,
                          Map<String, IndexerConfig> indexerConfigs,
                          Map<String, EvaluatorConfig> evaluatorConfigs,
                          Map<String, SchedulerConfig> schedulerConfigs,
                          RouterConfig routerConfig) {
        this.retrieverConfigs = retrieverConfigs;
        this.predictorConfigs = predictorConfigs;
        this.indexerConfigs = indexerConfigs;
        this.evaluatorConfigs = evaluatorConfigs;
        this.schedulerConfigs = schedulerConfigs;
        this.routerConfig = routerConfig;
    }

    public Map<String, RetrieverConfig> getRetrieverConfigs() {
        return retrieverConfigs;
    }
    public Map<String, PredictorConfig> getPredictorConfigs() {
        return predictorConfigs;
    }
    public Map<String, RankerConfig> getRankerConfigs() {
        throw new ConfigurationException("Ranker is not supported in a PredictorEngine");
    }
    public Map<String, RecommenderConfig> getRecommenderConfigs() {
        throw new ConfigurationException("Recommender is not supported in a PredictorEngine");
    }
    public Map<String, IndexerConfig> getIndexerConfigs() {
        return indexerConfigs;
    }
    public Map<String, EvaluatorConfig> getEvaluatorConfigs() {
        return evaluatorConfigs;
    }
    public Map<String, SchedulerConfig> getSchedulerConfigs() {
        return schedulerConfigs;
    }
    public RouterConfig getRouterConfig() {
        return routerConfig;
    }
}
