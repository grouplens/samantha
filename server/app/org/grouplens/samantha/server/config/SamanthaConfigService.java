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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.grouplens.samantha.server.evaluator.Evaluator;
import org.grouplens.samantha.server.exception.ConfigurationException;

import org.grouplens.samantha.server.indexer.Indexer;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.scheduler.QuartzSchedulerService;
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
    final private Map<String, EngineConfig> namedEngineConfig = new HashMap<>();

    @Inject
    private SamanthaConfigService(Configuration configuration,
                                  Injector injector)
            throws ConfigurationException {
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
            Configuration engineConfig = config.
                    getConfig(ConfigKey.SAMANTHA_BASE.get() + "." + engine);
            String engineType = engineConfig.getString(ConfigKey.ENGINE_TYPE.get());
            namedEngineConfig.put(engine,
                    EngineType.valueOf(engineType).loadConfig(engine, engineConfig, injector));
        }
    }

    public void reloadConfig() {
        ConfigFactory.invalidateCaches();
        Config config = ConfigFactory.load();
        loadConfig(new Configuration(config));
    }

    public Configuration getConfig() {
        Config config = ConfigFactory.load();
        Configuration configuration = new Configuration(config);
        return configuration.getConfig(ConfigKey.SAMANTHA_BASE.get());
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

    public Router getRouter(String engineName, RequestContext requestContext) {
        return namedEngineConfig.get(engineName).getRouterConfig().getRouter(requestContext);
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
