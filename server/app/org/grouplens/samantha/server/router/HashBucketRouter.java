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

package org.grouplens.samantha.server.router;

import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.Predictor;
import org.grouplens.samantha.server.recommender.Recommender;
import play.Configuration;

import java.util.List;
import java.util.Map;

public class HashBucketRouter implements Router {
    private final List<String> predHashAttrs;
    private final List<String> recHashAttrs;
    private final Integer numPredBuckets;
    private final Integer numRecBuckets;
    private final Configuration predName2range;
    private final Configuration recName2range;

    public HashBucketRouter(List<String> predHashAttrs, List<String> recHashAttrs,
                            Integer numPredBuckets, Integer numRecBuckets,
                            Configuration predName2range, Configuration recName2range) {
        this.predHashAttrs = predHashAttrs;
        this.recHashAttrs = recHashAttrs;
        this.numPredBuckets = numPredBuckets;
        this.numRecBuckets = numRecBuckets;
        this.predName2range = predName2range;
        this.recName2range = recName2range;
    }

    private String route(RequestContext requestContext, List<String> hashAttrs, Integer numBuckets,
                         Configuration name2range) {
        String key = FeatureExtractorUtilities.composeConcatenatedKey(requestContext.getRequestBody(), hashAttrs);
        Integer bucket = key.hashCode() % numBuckets;
        String hit = null;
        for (String name : name2range.keys()) {
            List<Integer> ranges = name2range.getIntList(name);
            for (int i=0; i<ranges.size(); i+=2) {
                if (bucket >= ranges.get(i) && bucket <= ranges.get(i+1)) {
                    hit = name;
                }
            }
        }
        return hit;
    }

    public Recommender routeRecommender(Map<String, Recommender> recommenders,
                                        RequestContext requestContext) {
        return recommenders.get(route(requestContext, recHashAttrs, numRecBuckets, recName2range));
    }

    public Predictor routePredictor(Map<String, Predictor> predictors,
                                    RequestContext requestContext) {
        return predictors.get(route(requestContext, predHashAttrs, numPredBuckets, predName2range));
    }
}
