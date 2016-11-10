package org.grouplens.samantha.server.router;

import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.Predictor;
import org.grouplens.samantha.server.recommender.Recommender;

import java.util.Map;

public interface Router {
    Recommender routeRecommender(Map<String, Recommender> recommenders,
                                 RequestContext requestContext);
    Predictor routePredictor(Map<String, Predictor> predictors,
                             RequestContext requestContext);
}
