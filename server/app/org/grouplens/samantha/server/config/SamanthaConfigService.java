/*
 * Copyright (c) [2016-2018] [University of Minnesota]
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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.grouplens.samantha.server.evaluator.Evaluator;
import org.grouplens.samantha.server.evaluator.EvaluatorConfig;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.exception.ConfigurationException;

import org.grouplens.samantha.server.indexer.Indexer;
import org.grouplens.samantha.server.indexer.IndexerConfig;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.ranker.RankerConfig;
import org.grouplens.samantha.server.retriever.RetrieverConfig;
import org.grouplens.samantha.server.scheduler.QuartzSchedulerService;
import org.grouplens.samantha.server.predictor.Predictor;
import org.grouplens.samantha.server.predictor.PredictorConfig;
import org.grouplens.samantha.server.ranker.Ranker;
import org.grouplens.samantha.server.recommender.Recommender;
import org.grouplens.samantha.server.recommender.RecommenderConfig;
import org.grouplens.samantha.server.retriever.Retriever;
import org.grouplens.samantha.server.router.Router;
import org.grouplens.samantha.server.scheduler.SchedulerConfig;
import play.Configuration;
import play.inject.Injector;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class SamanthaConfigService {
    private Configuration configuration;
    final private Injector injector;
    final private Map<String, EngineConfig> namedEngineConfig = new HashMap<>();

    @Inject
    private SamanthaConfigService(Configuration configuration,
                                  Injector injector)
            throws ConfigurationException {
        this.configuration = configuration;
        this.injector = injector;
        loadConfig(configuration);
    }

    private void loadConfig(Configuration config) {
        QuartzSchedulerService jobService = injector.instanceOf(QuartzSchedulerService.class);
        jobService.clearAllJobs();
        namedEngineConfig.clear();
        List<String> enabledEngines = config.
                getStringList(ConfigKey.ENGINES_ENABLED.get());
        for (String engine : enabledEngines) {
            String enginePath = ConfigKey.SAMANTHA_BASE.get() + "." + engine;
            Configuration engineConfig = config.getConfig(enginePath);
            if (engineConfig == null) {
                throw new ConfigurationException("The enabled engine " + enginePath + " does not exist!");
            }
            String engineType = engineConfig.getString(ConfigKey.ENGINE_TYPE.get());
            namedEngineConfig.put(engine,
                    EngineType.valueOf(engineType).loadConfig(engine, engineConfig, injector));
        }
    }

    public void reloadConfig() {
        ConfigFactory.invalidateCaches();
        Config config = ConfigFactory.load();
        configuration = new Configuration(config);
        loadConfig(configuration);
    }

    public void setConfig(Configuration config) {
        configuration = config;
        loadConfig(config);
    }

    public Configuration getConfig() {
        return configuration.getConfig(ConfigKey.SAMANTHA_BASE.get());
    }

    private String checkEngine(RequestContext requestContext) {
        String engineName = requestContext.getEngineName();
        EngineConfig engineConfig = namedEngineConfig.get(engineName);
        checkNullWithTypeName(engineConfig, "engine", engineName);
        return engineName;
    }

    private void checkNullWithTypeName(Object config, String type, String name) {
        if (config == null) {
            throw new BadRequestException("The requested " + type + " " + name + " does not exist!");
        }
    }

    public Indexer getIndexer(String indexerName, RequestContext requestContext) {
        String engineName = checkEngine(requestContext);
        IndexerConfig indexerConfig = namedEngineConfig.get(engineName)
                .getIndexerConfigs().get(indexerName);
        checkNullWithTypeName(indexerConfig, "indexer", indexerName);
        return indexerConfig.getIndexer(requestContext);
    }

    public Retriever getRetriever(String retrieverName, RequestContext requestContext) {
        String engineName = checkEngine(requestContext);
        RetrieverConfig retrieverConfig = namedEngineConfig.get(engineName)
                .getRetrieverConfigs().get(retrieverName);
        checkNullWithTypeName(retrieverConfig, "retriever", retrieverName);
        return retrieverConfig.getRetriever(requestContext);
    }

    public Predictor getPredictor(String predictorName, RequestContext requestContext) {
        String engineName = checkEngine(requestContext);
        PredictorConfig predictorConfig = namedEngineConfig.get(engineName)
                .getPredictorConfigs().get(predictorName);
        checkNullWithTypeName(predictorConfig, "predictor", predictorName);
        return predictorConfig.getPredictor(requestContext);
    }

    public Ranker getRanker(String rankerName, RequestContext requestContext) {
        String engineName = checkEngine(requestContext);
        RankerConfig rankerConfig = namedEngineConfig.get(engineName)
                .getRankerConfigs().get(rankerName);
        checkNullWithTypeName(rankerConfig, "ranker", rankerName);
        return rankerConfig.getRanker(requestContext);
    }

    public Evaluator getEvaluator(String evalName, RequestContext requestContext) {
        String engineName = checkEngine(requestContext);
        EvaluatorConfig evaluatorConfig = namedEngineConfig.get(engineName)
                .getEvaluatorConfigs().get(evalName);
        checkNullWithTypeName(evaluatorConfig, "evaluator", evalName);
        return evaluatorConfig.getEvaluator(requestContext);
    }

    public Recommender getRecommender(String recommenderName, RequestContext requestContext) {
        String engineName = checkEngine(requestContext);
        RecommenderConfig recommenderConfig = namedEngineConfig.get(engineName)
                .getRecommenderConfigs().get(recommenderName);
        checkNullWithTypeName(recommenderConfig, "recommender", recommenderName);
        return recommenderConfig.getRecommender(requestContext);
    }

    public SchedulerConfig getSchedulerConfig(String schedulerName, RequestContext requestContext) {
        String engineName = checkEngine(requestContext);
        SchedulerConfig schedulerConfig = namedEngineConfig.get(engineName)
                .getSchedulerConfigs().get(schedulerName);
        checkNullWithTypeName(schedulerConfig, "scheduler", schedulerName);
        return schedulerConfig;
    }

    public Router getRouter(RequestContext requestContext) {
        String engineName = checkEngine(requestContext);
        return namedEngineConfig.get(engineName).getRouterConfig().getRouter(requestContext);
    }

    public Predictor routePredictor(RequestContext requestContext) {
        String engineName = checkEngine(requestContext);
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
        String engineName = checkEngine(requestContext);
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
