package org.grouplens.samantha.server.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.predictor.Predictor;
import org.grouplens.samantha.server.ranker.RankedResult;
import org.grouplens.samantha.server.recommender.Recommender;
import play.libs.Json;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class ResponsePacker {

    @Inject
    private ResponsePacker() {}

    public JsonNode packRecommendation(Recommender recommender, RankedResult rankedResult,
                                       RequestContext requestContext) {
        ObjectNode result = Json.newObject();
        result.set("recommendations", rankedResult.toJson());
        result.set("configuration", Json.toJson(recommender.getConfig().asMap()));
        result.put("engine", requestContext.getEngineName());
        return result;
    }
    public JsonNode packPrediction(Predictor predictor, List<Prediction> predictedResult,
                                   RequestContext requestContext) {
        ObjectNode result = Json.newObject();
        ArrayNode predictions = Json.newArray();
        for (Prediction pred : predictedResult) {
            predictions.add(pred.toJson());
        }
        result.set("predictions", predictions);
        result.set("configuration", Json.toJson(predictor.getConfig().asMap()));
        result.put("engine", requestContext.getEngineName());
        return result;
    }
}
