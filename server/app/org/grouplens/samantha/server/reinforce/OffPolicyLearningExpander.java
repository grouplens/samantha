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

package org.grouplens.samantha.server.reinforce;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.ranker.RankedResult;
import org.grouplens.samantha.server.recommender.Recommender;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OffPolicyLearningExpander implements EntityExpander {
    final private String recommenderName;
    final private Double sampleRate;
    final private List<String> itemAttrs;
    final private Injector injector;

    public OffPolicyLearningExpander(String recommenderName, Double sampleRate,
                                     List<String> itemAttrs, Injector injector) {
        this.recommenderName = recommenderName;
        this.injector = injector;
        this.sampleRate = sampleRate;
        this.itemAttrs = itemAttrs;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        return new OffPolicyLearningExpander(expanderConfig.getString("recommenderName"),
                expanderConfig.getDouble("sampleRate"),
                expanderConfig.getStringList("itemAttrs"), injector);
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        List<ObjectNode> expandedResult = new ArrayList<>();
        for (ObjectNode input : initialResult) {
            if (sampleRate == null || new Random().nextDouble() <= sampleRate) {
                ObjectNode reqBody = Json.newObject();
                reqBody.setAll(input);
                RequestContext pseudoReq = new RequestContext(reqBody, requestContext.getEngineName());
                SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
                Recommender recommender = configService.getRecommender(recommenderName, pseudoReq);
                RankedResult rankedResult = recommender.recommend(pseudoReq);
                if (rankedResult.getLimit() > 0) {
                    String itemKey = FeatureExtractorUtilities.composeConcatenatedKey(input, itemAttrs);
                    for (Prediction prediction : rankedResult.getRankingList()) {
                        String recKey = FeatureExtractorUtilities.composeConcatenatedKey(
                                prediction.getEntity(),
                                itemAttrs
                        );
                        if (itemKey.equals(recKey)) {
                            expandedResult.add(input);
                        }
                    }
                }
            }
        }
        return expandedResult;
    }
}
