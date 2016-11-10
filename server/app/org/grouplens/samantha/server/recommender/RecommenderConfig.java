package org.grouplens.samantha.server.recommender;

import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public interface RecommenderConfig {
    static RecommenderConfig getRecommenderConfig(Configuration recommenderConfig,
                                                  Injector injector) {return null;}
    Recommender getRecommender(RequestContext requestContext);
}
