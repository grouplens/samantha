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