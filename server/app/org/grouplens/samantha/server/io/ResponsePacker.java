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

    public JsonNode packRecommendation(Recommender recommender, RankedResult rankedResult) {
        ObjectNode result = Json.newObject();
        result.set("recommendations", rankedResult.toJson());
        result.set("footprint", recommender.getFootprint());
        return result;
    }
    public JsonNode packPrediction(Predictor predictor, List<Prediction> predictedResult) {
        ObjectNode result = Json.newObject();
        ArrayNode predictions = Json.newArray();
        for (Prediction pred : predictedResult) {
            predictions.add(pred.toJson());
        }
        result.set("predictions", predictions);
        result.set("footprint", predictor.getFootprint());
        return result;
    }
}
