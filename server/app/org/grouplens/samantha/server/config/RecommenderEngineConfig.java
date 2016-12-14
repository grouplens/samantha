package org.grouplens.samantha.server.config;

import org.grouplens.samantha.server.evaluator.EvaluatorConfig;
import org.grouplens.samantha.server.indexer.IndexerConfig;
import org.grouplens.samantha.server.predictor.PredictorConfig;
import org.grouplens.samantha.server.ranker.RankerConfig;
import org.grouplens.samantha.server.recommender.RecommenderConfig;
import org.grouplens.samantha.server.retriever.RetrieverConfig;
import org.grouplens.samantha.server.router.RouterConfig;
import org.grouplens.samantha.server.scheduler.SchedulerConfig;

import java.util.Map;

class RecommenderEngineConfig extends PredictorEngineConfig {
    private final Map<String, RankerConfig> rankerConfigs;
    private final Map<String, RecommenderConfig> recommenderConfigs;

    RecommenderEngineConfig(Map<String, RetrieverConfig> retrieverConfigs,
                            Map<String, PredictorConfig> predictorConfigs,
                            Map<String, RankerConfig> rankerConfigs,
                            Map<String, RecommenderConfig> recommenderConfigs,
                            Map<String, IndexerConfig> indexerConfigs,
                            Map<String, EvaluatorConfig> evaluatorConfigs,
                            Map<String, SchedulerConfig> schedulerConfigs,
                            RouterConfig routerConfig) {
        super(retrieverConfigs, predictorConfigs, indexerConfigs, evaluatorConfigs,
                schedulerConfigs, routerConfig);
        this.rankerConfigs = rankerConfigs;
        this.recommenderConfigs = recommenderConfigs;
    }

    public Map<String, RankerConfig> getRankerConfigs() {
        return rankerConfigs;
    }
    public Map<String, RecommenderConfig> getRecommenderConfigs() {
        return recommenderConfigs;
    }
}
