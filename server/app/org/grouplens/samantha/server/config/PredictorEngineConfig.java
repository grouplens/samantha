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
