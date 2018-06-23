/*
 * Copyright (c) [2016-2018] [University of Minnesota]
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
