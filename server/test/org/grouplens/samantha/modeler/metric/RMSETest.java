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

package org.grouplens.samantha.modeler.metric;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.predictor.Prediction;
import org.junit.Test;
import play.libs.Json;

import java.util.List;

import static org.junit.Assert.*;

public class RMSETest {

    @Test
    public void testRMSE() {
        RMSE rmse = new RMSE("rating", 5.0);
        ObjectNode gt1 = Json.newObject().put("item", 1).put("rating", 3.0);
        ObjectNode gt2 = Json.newObject().put("item", 3).put("rating", 4.5);
        ObjectNode gt3 = Json.newObject().put("item", 5).put("rating", 2.0);
        List<ObjectNode> gts1 = Lists.newArrayList(gt1, gt2, gt3);
        Prediction pred1 = new Prediction(
                gt1, null, 2.5, null);
        Prediction pred2 = new Prediction(
                gt2, null, 4.5, null);
        Prediction pred3 = new Prediction(
                gt3, null, 1.0, null);
        List<Prediction> preds1 = Lists.newArrayList(pred1, pred2, pred3);
        rmse.add(gts1, preds1);
        MetricResult results = rmse.getResults();
        assertEquals(true, results.getPass());
        assertEquals(1, results.getValues().size());
        JsonNode result = results.getValues().get(0);
        int support = result.get(ConfigKey.EVALUATOR_METRIC_SUPPORT.get()).asInt();
        double value = result.get(ConfigKey.EVALUATOR_METRIC_VALUE.get()).asDouble();
        assertEquals(3, support);
        assertEquals(0.645, value, 0.001);
    }
}
