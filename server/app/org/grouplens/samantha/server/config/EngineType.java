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
import play.Configuration;
import play.inject.Injector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public enum EngineType implements EngineConfigLoader {

    RECOMMENDER("recommender") {
        public EngineConfig loadConfig(String engineName, Configuration engineConfig, Injector injector)
                throws ConfigurationException {
            Map<String, RetrieverConfig> retrieverConfigs = getRetrieverConfigs(engineConfig,
                    injector);
            Map<String, PredictorConfig> predictorConfigs = getPredictorConfigs(engineConfig,
                    injector);
            Map<String, RankerConfig> rankerConfigs = getRankerConfigs(engineConfig,
                    injector);
            Map<String, RecommenderConfig> recommenderConfigs =
                    getRecommenderConfigs(engineConfig, injector);
            Map<String, IndexerConfig> indexerConfigs =
                    getIndexerConfigs(engineConfig, injector);
            Map<String, EvaluatorConfig> evaluatorConfigs =
                    getEvaluatorConfigs(engineConfig, injector);
            Map<String, SchedulerConfig> schedulerConfigs =
                    getSchedulerConfigs(engineConfig, injector, engineName);
            RouterConfig routerConfig = getRouterConfig(engineConfig, injector);
            return new RecommenderEngineConfig(retrieverConfigs,
                    predictorConfigs, rankerConfigs, recommenderConfigs,
                    indexerConfigs, evaluatorConfigs, schedulerConfigs, routerConfig);
        }
    },
    PREDICTOR("predictor") {
        public EngineConfig loadConfig(String engineName, Configuration engineConfig, Injector injector)
            throws ConfigurationException {
            Map<String, RetrieverConfig> retrieverConfigs =
                    getRetrieverConfigs(engineConfig, injector);
            Map<String, PredictorConfig> predictorConfigs =
                    getPredictorConfigs(engineConfig, injector);
            Map<String, IndexerConfig> indexerConfigs =
                    getIndexerConfigs(engineConfig, injector);
            Map<String, EvaluatorConfig> evaluatorConfigs =
                    getEvaluatorConfigs(engineConfig, injector);
            Map<String, SchedulerConfig> schedulerConfigs =
                    getSchedulerConfigs(engineConfig, injector, engineName);
            RouterConfig routerConfig = getRouterConfig(engineConfig, injector);
            return new PredictorEngineConfig(retrieverConfigs,
                    predictorConfigs, indexerConfigs, evaluatorConfigs,
                    schedulerConfigs, routerConfig);
        }
    };

    private final String engineType;

    EngineType(String engineType) {
        this.engineType = engineType;
    }

    public String get() {
        return engineType;
    }

    protected Map<String, RetrieverConfig> getRetrieverConfigs (
            Configuration engineConfig, Injector injector)
            throws ConfigurationException {
        try {
            Map<String, RetrieverConfig> retrieverConfigs = new HashMap<>();
            for (Configuration retrConfig : engineConfig
                    .getConfigList(EngineComponent.RETRIEVER.get())) {
                String retrConfigClass = retrConfig.
                        getString(ConfigKey.ENGINE_COMPONENT_CONFIG_CLASS.get());
                Method method = Class.forName(retrConfigClass)
                        .getMethod("getRetrieverConfig", Configuration.class,
                                Injector.class);
                retrieverConfigs.put(
                        retrConfig.getString(ConfigKey.ENGINE_COMPONENT_NAME.get()),
                        (RetrieverConfig) method.invoke(null, retrConfig, injector));
            }
            return retrieverConfigs;
        } catch (IllegalAccessException | InvocationTargetException |
                ClassNotFoundException | NoSuchMethodException e) {
            throw new ConfigurationException(e);
        }
    }

    protected Map<String, PredictorConfig> getPredictorConfigs (
            Configuration engineConfig, Injector injector)
            throws ConfigurationException {
        try {
            Map<String, PredictorConfig> predictorConfigs = new HashMap<>();
            for (Configuration predConfig : engineConfig
                    .getConfigList(EngineComponent.PREDICTOR.get())) {
                String predConfigClass = predConfig.
                        getString(ConfigKey.ENGINE_COMPONENT_CONFIG_CLASS.get());
                Method method = Class.forName(predConfigClass)
                        .getMethod("getPredictorConfig", Configuration.class,
                                Injector.class);
                predictorConfigs.put(
                        predConfig.getString(ConfigKey.ENGINE_COMPONENT_NAME.get()),
                        (PredictorConfig) method.invoke(null, predConfig, injector));
            }
            return predictorConfigs;
        } catch (IllegalAccessException | InvocationTargetException |
                ClassNotFoundException | NoSuchMethodException e) {
            throw new ConfigurationException(e);
        }
    }

    protected Map<String, RankerConfig> getRankerConfigs (
            Configuration engineConfig, Injector injector)
            throws ConfigurationException {
        try {
            Map<String, RankerConfig> rankerConfigs = new HashMap<>();
            for (Configuration rankConfig : engineConfig
                    .getConfigList(EngineComponent.RANKER.get())) {
                String rankConfigClass = rankConfig.
                        getString(ConfigKey.ENGINE_COMPONENT_CONFIG_CLASS.get());
                Method method = Class.forName(rankConfigClass)
                        .getMethod("getRankerConfig", Configuration.class,
                                Injector.class);
                rankerConfigs.put(
                        rankConfig.getString(ConfigKey.ENGINE_COMPONENT_NAME.get()),
                        (RankerConfig) method.invoke(null, rankConfig, injector));
            }
            return rankerConfigs;
        } catch (IllegalAccessException | InvocationTargetException |
                ClassNotFoundException | NoSuchMethodException e) {
            throw new ConfigurationException(e);
        }
    }

    protected Map<String, RecommenderConfig> getRecommenderConfigs(
            Configuration engineConfig, Injector injector)
            throws ConfigurationException {
        try {
            Map<String, RecommenderConfig> recommenderConfigs = new HashMap<>();
            for (Configuration recConfig : engineConfig
                    .getConfigList(EngineComponent.RECOMMENDER.get())) {
                String recConfigClass = recConfig.
                        getString(ConfigKey.ENGINE_COMPONENT_CONFIG_CLASS.get());
                Method method = Class.forName(recConfigClass)
                        .getMethod("getRecommenderConfig", Configuration.class,
                                Injector.class);
                recommenderConfigs.put(
                        recConfig.getString(ConfigKey.ENGINE_COMPONENT_NAME.get()),
                        (RecommenderConfig) method.invoke(null, recConfig, injector));
            }
            return recommenderConfigs;
        } catch (IllegalAccessException | InvocationTargetException |
                ClassNotFoundException | NoSuchMethodException e) {
            throw new ConfigurationException(e);
        }
    }

    protected Map<String, EvaluatorConfig> getEvaluatorConfigs (
            Configuration engineConfig, Injector injector)
            throws ConfigurationException {
        try {
            Map<String, EvaluatorConfig> evaluatorConfigs = new HashMap<>();
            for (Configuration evaluatorConfig : engineConfig
                    .getConfigList(EngineComponent.EVALUATOR.get())) {
                String evaluatorConfigClass = evaluatorConfig.
                        getString(ConfigKey.ENGINE_COMPONENT_CONFIG_CLASS.get());
                Method method = Class.forName(evaluatorConfigClass)
                        .getMethod("getEvaluatorConfig", Configuration.class,
                                Injector.class);
                evaluatorConfigs.put(
                        evaluatorConfig.getString(ConfigKey.ENGINE_COMPONENT_NAME.get()),
                        (EvaluatorConfig) method.invoke(null, evaluatorConfig, injector));
            }
            return evaluatorConfigs;
        } catch (IllegalAccessException | InvocationTargetException |
                ClassNotFoundException | NoSuchMethodException e) {
            throw new ConfigurationException(e);
        }
    }

    protected Map<String, SchedulerConfig> getSchedulerConfigs(
            Configuration engineConfig, Injector injector, String engineName)
            throws ConfigurationException {
        try {
            Map<String, SchedulerConfig> schedulerConfigs = new HashMap<>();
            for (Configuration schedulerConfig : engineConfig
                    .getConfigList(EngineComponent.SCHEDULER.get())) {
                String schedulerConfigClass = schedulerConfig.
                        getString(ConfigKey.ENGINE_COMPONENT_CONFIG_CLASS.get());
                Method method = Class.forName(schedulerConfigClass)
                        .getMethod("getSchedulerConfig", String.class,
                                Configuration.class, Injector.class);
                SchedulerConfig scheduler = (SchedulerConfig) method
                        .invoke(null, engineName, schedulerConfig, injector);
                schedulerConfigs.put(
                        schedulerConfig.getString(ConfigKey.ENGINE_COMPONENT_NAME.get()), scheduler);
                scheduler.scheduleJobs();
            }
            return schedulerConfigs;
        } catch (IllegalAccessException | InvocationTargetException |
                ClassNotFoundException | NoSuchMethodException e) {
            throw new ConfigurationException(e);
        }
    }

    protected RouterConfig getRouterConfig (
            Configuration engineConfig, Injector injector)
            throws ConfigurationException {
        try {
            Configuration routeConfig = engineConfig
                    .getConfig(EngineComponent.ROUTER.get());
            String routeConfigClass = routeConfig.
                    getString(ConfigKey.ENGINE_COMPONENT_CONFIG_CLASS.get());
            Method method = Class.forName(routeConfigClass)
                    .getMethod("getRouterConfig", Configuration.class,
                            Injector.class);
            return (RouterConfig) method.invoke(null, routeConfig, injector);
        } catch (IllegalAccessException | InvocationTargetException |
                ClassNotFoundException | NoSuchMethodException e) {
            throw new ConfigurationException(e);
        }
    }

    protected Map<String, IndexerConfig> getIndexerConfigs(
            Configuration engineConfig, Injector injector)
            throws ConfigurationException {
        try {
            Map<String, IndexerConfig> indexerConfigs = new HashMap<>();
            for (Configuration indConfig : engineConfig
                    .getConfigList(EngineComponent.INDEXER.get())) {
                String indConfigClass = indConfig.
                        getString(ConfigKey.ENGINE_COMPONENT_CONFIG_CLASS.get());
                Method method = Class.forName(indConfigClass)
                        .getMethod("getIndexerConfig", Configuration.class,
                                Injector.class);
                indexerConfigs.put(
                        indConfig.getString(ConfigKey.ENGINE_COMPONENT_NAME.get()),
                        (IndexerConfig) method.invoke(null, indConfig, injector));
            }
            return indexerConfigs;
        } catch (IllegalAccessException | InvocationTargetException |
                ClassNotFoundException | NoSuchMethodException e) {
            throw new ConfigurationException(e);
        }
    }
}
