package org.grouplens.samantha.server.config;

import org.grouplens.samantha.server.evaluator.EvaluatorConfig;
import org.grouplens.samantha.server.indexer.IndexerConfig;
import org.grouplens.samantha.server.predictor.PredictorConfig;
import org.grouplens.samantha.server.ranker.RankerConfig;
import org.grouplens.samantha.server.recommender.RecommenderConfig;
import org.grouplens.samantha.server.retriever.RetrieverConfig;
import org.grouplens.samantha.server.router.RouterConfig;

import java.util.Map;

class RecommenderEngineConfig implements EngineConfig {
    private final Map<String, RetrieverConfig> retrieverConfigs;
    private final Map<String, PredictorConfig> predictorConfigs;
    private final Map<String, RankerConfig> rankerConfigs;
    private final Map<String, RecommenderConfig> recommenderConfigs;
    private final Map<String, IndexerConfig> indexerConfigs;
    private final Map<String, EvaluatorConfig> evaluatorConfigs;
    private final RouterConfig routerConfig;

    RecommenderEngineConfig(Map<String, RetrieverConfig> retrieverConfigs,
                            Map<String, PredictorConfig> predictorConfigs,
                            Map<String, RankerConfig> rankerConfigs,
                            Map<String, RecommenderConfig> recommenderConfigs,
                            Map<String, IndexerConfig> indexerConfigs,
                            Map<String, EvaluatorConfig> evaluatorConfigs,
                            RouterConfig routerConfig) {
        this.retrieverConfigs = retrieverConfigs;
        this.predictorConfigs = predictorConfigs;
        this.rankerConfigs = rankerConfigs;
        this.recommenderConfigs = recommenderConfigs;
        this.indexerConfigs = indexerConfigs;
        this.evaluatorConfigs = evaluatorConfigs;
        this.routerConfig = routerConfig;
    }

    public Map<String, RetrieverConfig> getRetrieverConfigs() {
        return retrieverConfigs;
    }
    public Map<String, PredictorConfig> getPredictorConfigs() {
        return predictorConfigs;
    }
    public Map<String, RankerConfig> getRankerConfigs() {
        return rankerConfigs;
    }
    public Map<String, RecommenderConfig> getRecommenderConfigs() {
        return recommenderConfigs;
    }
    public Map<String, IndexerConfig> getIndexerConfigs() {
        return indexerConfigs;
    }
    public Map<String, EvaluatorConfig> getEvaluatorConfigs() {
        return evaluatorConfigs;
    }
    public RouterConfig getRouterConfig() {
        return routerConfig;
    }
}
