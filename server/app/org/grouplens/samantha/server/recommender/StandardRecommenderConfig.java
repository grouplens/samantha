package org.grouplens.samantha.server.recommender;

import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class StandardRecommenderConfig implements RecommenderConfig {
    final private Injector injector;
    final private String retrieverName;
    final private String rankerName;
    final private Configuration config;

    private StandardRecommenderConfig(Configuration config,
                                      Injector injector,
                                      String retrieverName,
                                      String rankerName) {
        this.injector = injector;
        this.retrieverName = retrieverName;
        this.rankerName = rankerName;
        this.config = config;
    }

    public static RecommenderConfig getRecommenderConfig(Configuration recommenderConfig,
                                                         Injector injector) {
        String retrieverName = recommenderConfig.getString("retriever");
        String rankerName = recommenderConfig.getString("ranker");
        return new StandardRecommenderConfig(recommenderConfig, injector, retrieverName, rankerName);
    }

    public Recommender getRecommender(RequestContext requestContext) {
        SamanthaConfigService configService = injector
                .instanceOf(SamanthaConfigService.class);
        return new StandardRecommender(config, configService.getRetriever(retrieverName,
                requestContext), configService.getRanker(rankerName, requestContext));
    }
}
