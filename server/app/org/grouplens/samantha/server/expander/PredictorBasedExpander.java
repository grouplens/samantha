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

package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.predictor.Predictor;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class PredictorBasedExpander implements EntityExpander {
    private final Predictor predictor;
    private final String scoreAttr;
    private final String scoresAttr;

    public PredictorBasedExpander(Predictor predictor, String scoreAttr, String scoresAttr) {
        this.predictor = predictor;
        this.scoreAttr = scoreAttr;
        this.scoresAttr = scoresAttr;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                      Injector injector, RequestContext requestContext) {
        String predictorName = expanderConfig.getString("predictorName");
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        Predictor predictor = configService.getPredictor(predictorName, requestContext);
        return new PredictorBasedExpander(predictor, expanderConfig.getString("scoreAttr"),
                expanderConfig.getString("scoresAttr"));
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                            RequestContext requestContext){
        List<Prediction> predictions = predictor.predict(initialResult, requestContext);
        for (int i=0; i<predictions.size(); i++) {
            ObjectNode entity = initialResult.get(i);
            entity.put(scoreAttr, predictions.get(i).getScore());
            if (scoresAttr != null) {
                List<String> scoreStrArr = new ArrayList<>();
                double[] scores = predictions.get(i).getScores();
                for (int j=0; j<scores.length; j++) {
                    scoreStrArr.add(Double.toString(scores[j]));
                }
                entity.put(scoresAttr, StringUtils.join(scoreStrArr, ","));
            }
        }
        return initialResult;
    }
}
