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
