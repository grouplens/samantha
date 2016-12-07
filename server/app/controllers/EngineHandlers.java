package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.evaluator.Evaluation;
import org.grouplens.samantha.server.evaluator.Evaluator;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.indexer.Indexer;
import org.grouplens.samantha.server.predictor.Predictor;
import org.grouplens.samantha.server.ranker.RankedResult;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.io.RequestParser;
import org.grouplens.samantha.server.io.ResponsePacker;
import org.grouplens.samantha.server.recommender.Recommender;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.List;

public class EngineHandlers extends Controller {

    private final RequestParser requestParser;
    private final ResponsePacker responsePacker;
    private final SamanthaConfigService samanthaConfigService;

    //TODO: add outlier condition tests for all codes

    @Inject
    public EngineHandlers(RequestParser requestParser,
                          ResponsePacker responsePacker,
                          SamanthaConfigService samanthaConfigService) {
        this.requestParser = requestParser;
        this.responsePacker = responsePacker;
        this.samanthaConfigService = samanthaConfigService;
    }

    public Result getRecommendation(String engine) throws BadRequestException {
        JsonNode body = request().body().asJson();
        RequestContext requestContext = requestParser.getJsonRequestContext(engine, body);
        Recommender recommender = samanthaConfigService.routeRecommender(requestContext);
        RankedResult rankedResult = recommender.recommend(requestContext);
        JsonNode data = responsePacker.packRecommendation(recommender, rankedResult, requestContext);
        ObjectNode resp = JsonHelpers.successJson();
        resp.set("data", data);
        return ok(resp);
    }

    public Result getPrediction(String engine) throws BadRequestException {
        JsonNode body = request().body().asJson();
        RequestContext requestContext = requestParser.getJsonRequestContext(engine, body);
        Predictor predictor = samanthaConfigService.routePredictor(requestContext);
        List<Prediction> predictedResult = predictor.predict(requestContext);
        JsonNode data = responsePacker.packPrediction(predictor, predictedResult, requestContext);
        ObjectNode resp = JsonHelpers.successJson();
        resp.set("data", data);
        return ok(resp);
    }

    public Result evaluate(String engine) throws BadRequestException {
        JsonNode body = request().body().asJson();
        RequestContext requestContext = requestParser.getJsonRequestContext(engine, body);
        String evaluatorName = JsonHelpers.getRequiredString(body, "evaluator");
        Evaluator evaluator = samanthaConfigService
                .getEvaluator(evaluatorName, requestContext);
        Evaluation results = evaluator.evaluate(requestContext);
        ObjectNode resp = JsonHelpers.successJson();
        resp.set("data", Json.toJson(results));
        return ok(resp);
    }

    public Result predictorModel(String engine) throws BadRequestException {
        JsonNode body = request().body().asJson();
        RequestContext requestContext = requestParser.getJsonRequestContext(engine, body);
        String predictorName = JsonHelpers.getRequiredString(body, "predictor");
        samanthaConfigService.getPredictor(predictorName, requestContext);
        ObjectNode resp = JsonHelpers.successJson();
        return ok(resp);
    }

    public Result retrieverModel(String engine) throws BadRequestException {
        JsonNode body = request().body().asJson();
        RequestContext requestContext = requestParser.getJsonRequestContext(engine, body);
        String retrieverName = JsonHelpers.getRequiredString(body, "retriever");
        samanthaConfigService.getRetriever(retrieverName, requestContext);
        ObjectNode resp = JsonHelpers.successJson();
        return ok(resp);
    }

    public Result rankerModel(String engine) throws BadRequestException {
        JsonNode body = request().body().asJson();
        RequestContext requestContext = requestParser.getJsonRequestContext(engine, body);
        String rankerName = JsonHelpers.getRequiredString(body, "ranker");
        samanthaConfigService.getRanker(rankerName, requestContext);
        ObjectNode resp = JsonHelpers.successJson();
        return ok(resp);
    }

    public Result indexData(String engine) throws BadRequestException {
        JsonNode body = request().body().asJson();
        RequestContext requestContext = requestParser.getJsonRequestContext(engine, body);
        if (body.has("predictor")) {
            predictorModel(engine);
        }
        if (body.has("retriever")) {
            retrieverModel(engine);
        }
        if (body.has("ranker")) {
            rankerModel(engine);
        }
        boolean indexData = JsonHelpers.getOptionalBoolean(body, "indexData", true);
        if (indexData) {
            String indexerName = JsonHelpers.getRequiredString(body, "indexer");
            Indexer indexer = samanthaConfigService.getIndexer(indexerName, requestContext);
            indexer.index(requestContext);
        }
        ObjectNode resp = JsonHelpers.successJson();
        return ok(resp);
    }
}
