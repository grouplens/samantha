package org.grouplens.samantha.server.config;

import org.grouplens.samantha.server.evaluator.EvaluatorConfig;
import org.grouplens.samantha.server.exception.ConfigurationException;
import org.grouplens.samantha.server.indexer.IndexerConfig;
import org.grouplens.samantha.server.predictor.PredictorConfig;
import org.grouplens.samantha.server.ranker.RankerConfig;
import org.grouplens.samantha.server.recommender.RecommenderConfig;
import org.grouplens.samantha.server.retriever.RetrieverConfig;
import org.grouplens.samantha.server.router.RouterConfig;
import play.Configuration;
import play.inject.Injector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public enum EngineType implements EngineConfigLoader {

    RECOMMENDER("recommender") {
        public EngineConfig loadConfig(Configuration engineConfig, Injector injector)
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
            RouterConfig routerConfig = getRouterConfig(engineConfig, injector);
            return new RecommenderEngineConfig(retrieverConfigs,
                    predictorConfigs, rankerConfigs, recommenderConfigs,
                    indexerConfigs, evaluatorConfigs, routerConfig);
        }
    },
    PREDICTOR("predictor") {
        public EngineConfig loadConfig(Configuration engineConfig, Injector injector)
            throws ConfigurationException {
            Map<String, RetrieverConfig> retrieverConfigs =
                    getRetrieverConfigs(engineConfig, injector);
            Map<String, PredictorConfig> predictorConfigs =
                    getPredictorConfigs(engineConfig, injector);
            Map<String, IndexerConfig> indexerConfigs =
                    getIndexerConfigs(engineConfig, injector);
            Map<String, EvaluatorConfig> evaluatorConfigs =
                    getEvaluatorConfigs(engineConfig, injector);
            RouterConfig routerConfig = getRouterConfig(engineConfig, injector);
            return new PredictorEngineConfig(retrieverConfigs,
                    predictorConfigs, indexerConfigs, evaluatorConfigs, routerConfig);
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
                    .getConfigList(EngineComponent.RETRIEVERS.get())) {
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
                    .getConfigList(EngineComponent.PREDICTORS.get())) {
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
                    .getConfigList(EngineComponent.RANKERS.get())) {
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
                    .getConfigList(EngineComponent.RECOMMENDERS.get())) {
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
            for (Configuration rankConfig : engineConfig
                    .getConfigList(EngineComponent.EVALUATORS.get())) {
                String rankConfigClass = rankConfig.
                        getString(ConfigKey.ENGINE_COMPONENT_CONFIG_CLASS.get());
                Method method = Class.forName(rankConfigClass)
                        .getMethod("getEvaluatorConfig", Configuration.class,
                                Injector.class);
                evaluatorConfigs.put(
                        rankConfig.getString(ConfigKey.ENGINE_COMPONENT_NAME.get()),
                        (EvaluatorConfig) method.invoke(null, rankConfig, injector));
            }
            return evaluatorConfigs;
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
                    .getConfigList(EngineComponent.INDEXERS.get())) {
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
