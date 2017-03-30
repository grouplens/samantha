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
import org.grouplens.samantha.server.scheduler.SchedulerConfig;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.List;

/**
 * Handlers for requests to specific engines.
 *
 * This class is part of the play framework.
 * See conf/routes for how request urls and the methods here relate. Note that the implementation
 * of these handlers dictates the request processing flow of Samantha. Generally, it first gets
 * the right component from {@link SamanthaConfigService} and then ask the component to do the actual work.
 * A quick going through of the codes in this class in the concept level is enough to have a good
 * idea on how Samantha handles requests. Some handlers require certain keys to be present in the
 * body of the request, e.g. evaluator, predictor, retriever etc.
 */
public class EngineHandlers extends Controller {

    private final RequestParser requestParser;
    private final ResponsePacker responsePacker;
    private final SamanthaConfigService samanthaConfigService;

    /**
     * Constructor of EngineHandler.
     *
     * It is part of the play framework. It will be created (injected) by play framework
     * whenever relevant request urls come.
     *
     * @param requestParser must be injected with play injector. singleton.
     * @param responsePacker must be injected with play injector. singleton.
     * @param samanthaConfigService must be injected with play injector. singleton.
     */
    @Inject
    public EngineHandlers(RequestParser requestParser,
                          ResponsePacker responsePacker,
                          SamanthaConfigService samanthaConfigService) {
        this.requestParser = requestParser;
        this.responsePacker = responsePacker;
        this.samanthaConfigService = samanthaConfigService;
    }

    /**
      * Handler for recommendation request.
      *
      * The required keys are dictated by the configured router and the routed recommender, e.g.
      * {@link org.grouplens.samantha.server.router.BasicRouter BasicRouter} asks
      * for the key "recommender". The results are packed in "data", see
      * {@link ResponsePacker#packRecommendation(Recommender, RankedResult, RequestContext) packRecommendation}
      * for the detailed format of the formatted recommendations.
      *
      * @param engine the target engine name of this request.
      * @return a HTTP response with keys: status and data
      * @throws BadRequestException
      */
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

    /**
     * Handler for prediction request.
     *
     * The required keys are dictated by the configured router and the routed predictor, e.g.
     * {@link org.grouplens.samantha.server.router.BasicRouter BasicRouter} asks
     * for the key "predictor". The results are packed in "data",
     * see {@link ResponsePacker#packPrediction(Predictor, List, RequestContext) packPrediction}
     * for the detailed format of the formatted recommendations.
     *
     * @param engine the target engine name of this request.
     * @return a HTTP response with keys: status and data
     * @throws BadRequestException
     */
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

    /**
     * Handler for predictor or recommender evaluation request.
     *
     * It asks for the key "evaluator" in the request body. The corresponding evaluator will evaluate a default
     * recommender or predictor (or a recommender or predictor indicated by the request) configured by the evaluator.
     * It typically also asks for dao configuration, i.e. what data to use to evaluate.
     * See {@link org.grouplens.samantha.server.evaluator.RecommendationEvaluatorConfig RecommendationEvaluatorConfig}
     * or {@link org.grouplens.samantha.server.evaluator.PredictionEvaluatorConfig PredictionEvaluatorConfig} for the
     * details of how to use an evaluator combined with the evaluator configuration in an engine.
     *
     * @param engine the target engine name of this request.
     * @return a HTTP response with keys: status and data which wraps the evaluation results. See {@link Evaluation}
     * for the detailed format.
     * @throws BadRequestException
     */
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

    /**
     * Handler for predictor model management requests.
     *
     * It first asks for the key "predictor" in the request body, which tells which specific predictor implementation
     * it is involving from {@link SamanthaConfigService}.
     * The {@link org.grouplens.samantha.server.predictor.PredictorConfig#getPredictor(RequestContext) getPredictor}
     * method is called which typically asks for the key modelOperation and modelName in the request body. That's where
     * the actual model management task is executed.
     *
     * @param engine the target engine name of this request.
     * @return a HTTP response with the key "status" only (mostly with value "success" if the request is successfully processed).
     * @throws BadRequestException
     */
    public Result predictorModel(String engine) throws BadRequestException {
        JsonNode body = request().body().asJson();
        RequestContext requestContext = requestParser.getJsonRequestContext(engine, body);
        String predictorName = JsonHelpers.getRequiredString(body, "predictor");
        samanthaConfigService.getPredictor(predictorName, requestContext);
        ObjectNode resp = JsonHelpers.successJson();
        return ok(resp);
    }

    /**
     * Handler for retriever model management requests.
     *
     * It first asks for the key "retriever" in the request body, which tells which specific retriever implementation
     * it is involving from {@link SamanthaConfigService}.
     * The {@link org.grouplens.samantha.server.retriever.RetrieverConfig#getRetriever(RequestContext) getRetriever}
     * method is called which typically asks for the key modelOperation and modelName in the request body. That's where
     * the actual model management task is executed.
     *
     * @param engine the target engine name of this request.
     * @return a HTTP response with the key "status" only (mostly with value "success" if the request is successfully processed).
     * @throws BadRequestException
     */
    public Result retrieverModel(String engine) throws BadRequestException {
        JsonNode body = request().body().asJson();
        RequestContext requestContext = requestParser.getJsonRequestContext(engine, body);
        String retrieverName = JsonHelpers.getRequiredString(body, "retriever");
        samanthaConfigService.getRetriever(retrieverName, requestContext);
        ObjectNode resp = JsonHelpers.successJson();
        return ok(resp);
    }

    /**
     * Handler for ranker model management requests.
     *
     * It first asks for the key "ranker" in the request body, which tells which specific ranker implementation
     * it is involving from {@link SamanthaConfigService}.
     * The {@link org.grouplens.samantha.server.ranker.RankerConfig#getRanker(RequestContext) getRanker}
     * method is called which typically asks for the key modelOperation and modelName in the request body. That's where
     * the actual model management task is executed.
     *
     * @param engine the target engine name of this request.
     * @return a HTTP response with the key "status" only (mostly with value "success" if the request is successfully processed).
     * @throws BadRequestException
     */
    public Result rankerModel(String engine) throws BadRequestException {
        JsonNode body = request().body().asJson();
        RequestContext requestContext = requestParser.getJsonRequestContext(engine, body);
        String rankerName = JsonHelpers.getRequiredString(body, "ranker");
        samanthaConfigService.getRanker(rankerName, requestContext);
        ObjectNode resp = JsonHelpers.successJson();
        return ok(resp);
    }

    /**
     * Handler for data indexing requests.
     *
     * It first asks for the key "indexer" in the request body. Then the right indexer is found through {@link SamanthaConfigService}.
     * The found indexer then does the actual work of indexing data. How that indexer indexes data depends on the
     * specific implementation, i.e. what type of indexer and how it is configured in the engine configuration file.
     * After indexing the data, the data subscribers of the data are notified. Note that not the indexed data is passed
     * to the data subscribers (which might already have additional processed information), instead the raw data request is passed.
     *
     * @param engine the target engine name of this request.
     * @return a HTTP response with the key "status" only (mostly with value "success" if the request is successfully processed).
     * @throws BadRequestException
     */
    public Result indexData(String engine) throws BadRequestException {
        JsonNode body = request().body().asJson();
        RequestContext requestContext = requestParser.getJsonRequestContext(engine, body);
        String indexerName = JsonHelpers.getRequiredString(body, "indexer");
        Indexer indexer = samanthaConfigService.getIndexer(indexerName, requestContext);
        indexer.index(requestContext);
        indexer.notifyDataSubscribers(requestContext);
        ObjectNode resp = JsonHelpers.successJson();
        return ok(resp);
    }

    /**
     * Handler for running a scheduler's jobs.
     *
     * @param engine the target engine name of this request.
     * @return a HTTP response with the key "status" only (mostly with value "success" if the request is successfully processed).
     * @throws BadRequestException
     */
    public Result schedule(String engine) throws BadRequestException {
        JsonNode body = request().body().asJson();
        RequestContext requestContext = requestParser.getJsonRequestContext(engine, body);
        String schedulerName = JsonHelpers.getRequiredString(body, "scheduler");
        samanthaConfigService.getSchedulerConfig(schedulerName, requestContext).runJobs();
        ObjectNode resp = JsonHelpers.successJson();
        return ok(resp);
    }
}
