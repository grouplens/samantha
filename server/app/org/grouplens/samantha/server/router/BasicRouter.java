package org.grouplens.samantha.server.router;

import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.Predictor;
import org.grouplens.samantha.server.recommender.Recommender;

import java.util.Map;

public class BasicRouter implements Router {
    private final String recommenderKey;
    private final String predictorKey;

    public BasicRouter(String recommenderKey, String predictorKey) {
        this.recommenderKey = recommenderKey;
        this.predictorKey = predictorKey;
    }

    public Recommender routeRecommender(Map<String, Recommender> recommenders,
                                      RequestContext requestContext) {
        return recommenders.get(JsonHelpers
                .getRequiredString(requestContext.getRequestBody(),
                        recommenderKey));
    }

    public Predictor routePredictor(Map<String, Predictor> predictors,
                                  RequestContext requestContext) {
        return predictors.get(JsonHelpers
                .getRequiredString(requestContext.getRequestBody(),
                        predictorKey));
    }
}