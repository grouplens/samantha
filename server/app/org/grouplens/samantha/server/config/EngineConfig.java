package org.grouplens.samantha.server.config;

import org.grouplens.samantha.server.evaluator.EvaluatorConfig;
import org.grouplens.samantha.server.indexer.IndexerConfig;
import org.grouplens.samantha.server.predictor.PredictorConfig;
import org.grouplens.samantha.server.ranker.RankerConfig;
import org.grouplens.samantha.server.recommender.RecommenderConfig;
import org.grouplens.samantha.server.retriever.RetrieverConfig;
import org.grouplens.samantha.server.router.RouterConfig;

import java.util.Map;

interface EngineConfig {
    Map<String, IndexerConfig> getIndexerConfigs();
    Map<String, RetrieverConfig> getRetrieverConfigs();
    Map<String, PredictorConfig> getPredictorConfigs();
    Map<String, RankerConfig> getRankerConfigs();
    Map<String, EvaluatorConfig> getEvaluatorConfigs();
    Map<String, RecommenderConfig> getRecommenderConfigs();
    RouterConfig getRouterConfig();
}
