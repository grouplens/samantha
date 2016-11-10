package org.grouplens.samantha.server.recommender;

import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class StandardRecommenderConfig implements RecommenderConfig {
    final Injector injector;
    final private String retrieverName;
    final private String rankerName;
    final private String evaluatorName;

    private StandardRecommenderConfig(Injector injector,
                                      String retrieverName,
                                      String rankerName,
                                      String evaluatorName) {
        this.injector = injector;
        this.retrieverName = retrieverName;
        this.rankerName = rankerName;
        this.evaluatorName = evaluatorName;
    }

    public static RecommenderConfig getRecommenderConfig(Configuration recommenderConfig,
                                                  Injector injector) {
        String retrieverName = recommenderConfig.getString("retriever");
        String rankerName = recommenderConfig.getString("ranker");
        String evaluatorName = recommenderConfig.getString("evaluator");
        return new StandardRecommenderConfig(injector, retrieverName,
                rankerName, evaluatorName);
    }

    public Recommender getRecommender(RequestContext requestContext) {
        SamanthaConfigService configService = injector
                .instanceOf(SamanthaConfigService.class);
        return new StandardRecommender(configService.getRetriever(retrieverName,
                requestContext), configService.getRanker(rankerName, requestContext));
    }
}
