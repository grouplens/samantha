package org.grouplens.samantha.server.config;

import org.grouplens.samantha.server.evaluator.Evaluator;
import org.grouplens.samantha.server.exception.ConfigurationException;

import org.grouplens.samantha.server.indexer.Indexer;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.Predictor;
import org.grouplens.samantha.server.predictor.PredictorConfig;
import org.grouplens.samantha.server.ranker.Ranker;
import org.grouplens.samantha.server.recommender.Recommender;
import org.grouplens.samantha.server.recommender.RecommenderConfig;
import org.grouplens.samantha.server.retriever.Retriever;
import org.grouplens.samantha.server.router.Router;
import play.Configuration;
import play.inject.Injector;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class SamanthaConfigService {
    final private Injector injector;
    final private Configuration configuration;
    final private Map<String, EngineConfig> namedEngineConfig = new HashMap<>();

    @Inject
    private SamanthaConfigService(Configuration configuration,
                                  Injector injector)
            throws ConfigurationException {
        this.configuration = configuration;
        this.injector = injector;
        loadConfig();
    }

    private void loadConfig() throws ConfigurationException {
        namedEngineConfig.clear();
        List<String> enabledEngines = configuration.
                getStringList(ConfigKey.ENGINES_ENABLED.get());
        for (String engine : enabledEngines) {
            Configuration engineConfig = configuration.
                    getConfig(ConfigKey.SAMANTHA_BASE.get() + "." + engine);
            String engineType = engineConfig.getString(ConfigKey.ENGINE_TYPE.get());
            namedEngineConfig.put(engine,
                    EngineType.valueOf(engineType).loadConfig(engineConfig, injector));
        }
    }

    public Indexer getIndexer(String indexerName, RequestContext requestContext) {
        String engineName = requestContext.getEngineName();
        return namedEngineConfig.get(engineName)
                .getIndexerConfigs().get(indexerName).getIndexer(requestContext);
    }

    public Retriever getRetriever(String retrieverName, RequestContext requestContext) {
        String engineName = requestContext.getEngineName();
        return namedEngineConfig.get(engineName)
                .getRetrieverConfigs().get(retrieverName).getRetriever(requestContext);
    }

    public Predictor getPredictor(String predictorName, RequestContext requestContext) {
        String engineName = requestContext.getEngineName();
        return namedEngineConfig.get(engineName)
                .getPredictorConfigs().get(predictorName).getPredictor(requestContext);
    }

    public Ranker getRanker(String rankerName, RequestContext requestContext) {
        String engineName = requestContext.getEngineName();
        return namedEngineConfig.get(engineName)
                .getRankerConfigs().get(rankerName).getRanker(requestContext);
    }

    public Evaluator getEvaluator(String evalName, RequestContext requestContext) {
        String engineName = requestContext.getEngineName();
        return namedEngineConfig.get(engineName)
                .getEvaluatorConfigs().get(evalName).getEvaluator(requestContext);
    }

    public Recommender getRecommender(String recommenderName, RequestContext requestContext) {
        String engineName = requestContext.getEngineName();
        return namedEngineConfig.get(engineName)
                .getRecommenderConfigs().get(recommenderName)
                .getRecommender(requestContext);
    }

    public Predictor routePredictor(RequestContext requestContext) {
        String engineName = requestContext.getEngineName();
        Router router = namedEngineConfig.get(engineName)
                .getRouterConfig().getRouter(requestContext);
        Map<String, PredictorConfig> predictorConfigs = namedEngineConfig.get(engineName)
                .getPredictorConfigs();
        Map<String, Predictor> predictors = new HashMap<>();
        for (Map.Entry<String, PredictorConfig> entry : predictorConfigs.entrySet()) {
            predictors.put(
                    entry.getKey(),
                    entry.getValue().getPredictor(requestContext)
            );
        }
        return router.routePredictor(predictors, requestContext);
    }

    public Recommender routeRecommender(RequestContext requestContext) {
        String engineName = requestContext.getEngineName();
        Router router = namedEngineConfig.get(engineName)
                .getRouterConfig().getRouter(requestContext);
        Map<String, RecommenderConfig> recommenderConfigs = namedEngineConfig.get(engineName)
                .getRecommenderConfigs();
        Map<String, Recommender> recommenders = new HashMap<>();
        for (Map.Entry<String, RecommenderConfig> entry : recommenderConfigs.entrySet()) {
            recommenders.put(
                    entry.getKey(),
                    entry.getValue().getRecommender(requestContext)
            );
        }
        return router.routeRecommender(recommenders, requestContext);
    }
}
