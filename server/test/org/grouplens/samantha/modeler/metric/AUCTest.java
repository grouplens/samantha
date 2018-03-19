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

public class AUCTest {

    @Test
    public void testGlobalAUC() {
        AUC auc = new AUC("click", AUC.AUCType.GLOBAL, 0.5, 0.5);
        ObjectNode gt1 = Json.newObject().put("item", 1).put("click", true);
        ObjectNode gt2 = Json.newObject().put("item", 3).put("click", false);
        ObjectNode gt3 = Json.newObject().put("item", 5).put("click", false);
        ObjectNode gt4 = Json.newObject().put("item", 4).put("click", true);
        ObjectNode gt5 = Json.newObject().put("item", 2).put("click", true);
        List<ObjectNode> gts1 = Lists.newArrayList(gt1, gt2, gt3, gt4, gt5);
        Prediction pred1 = new Prediction(
                gt1, null, 3.0, null);
        Prediction pred2 = new Prediction(
                gt2, null, 2.5, null);
        Prediction pred3 = new Prediction(
                gt3, null, 2.0, null);
        Prediction pred4 = new Prediction(
                gt4, null, 1.0, null);
        Prediction pred5 = new Prediction(
                gt5, null, 0.0, null);
        List<Prediction> preds1 = Lists.newArrayList(pred1, pred2, pred3, pred4, pred5);
        auc.add(gts1, preds1);
        pred1 = new Prediction(gt1, null, 5.0, null);
        pred2 = new Prediction(gt4, null, 2.8, null);
        pred3 = new Prediction(gt2, null, 2.3, null);
        pred4 = new Prediction(gt5, null, 1.5, null);
        pred5 = new Prediction(gt3, null, 0.2, null);
        List<ObjectNode> gts2 = Lists.newArrayList(gt1, gt4, gt2, gt5, gt3);
        List<Prediction> preds2 = Lists.newArrayList(pred1, pred2, pred3, pred4, pred5);
        auc.add(gts2, preds2);
        MetricResult results = auc.getResults();
        assertEquals(true, results.getPass());
        assertEquals(1, results.getValues().size());
        JsonNode result = results.getValues().get(0);
        int support = result.get(ConfigKey.EVALUATOR_METRIC_SUPPORT.get()).asInt();
        double value = result.get(ConfigKey.EVALUATOR_METRIC_VALUE.get()).asDouble();
        assertEquals(10, support);
        assertEquals(0.583, value, 0.001);
    }

    @Test
    public void testPerGroupAUC() {
        AUC auc = new AUC("click", AUC.AUCType.PERGROUP, 0.5, 0.5);
        ObjectNode gt1 = Json.newObject().put("item", 1).put("click", true);
        ObjectNode gt2 = Json.newObject().put("item", 3).put("click", false);
        ObjectNode gt3 = Json.newObject().put("item", 5).put("click", false);
        ObjectNode gt4 = Json.newObject().put("item", 4).put("click", true);
        ObjectNode gt5 = Json.newObject().put("item", 2).put("click", true);
        List<ObjectNode> gts1 = Lists.newArrayList(gt1, gt2, gt3, gt4, gt5);
        Prediction pred1 = new Prediction(
                gt1, null, 3.0, null);
        Prediction pred2 = new Prediction(
                gt2, null, 2.5, null);
        Prediction pred3 = new Prediction(
                gt3, null, 2.0, null);
        Prediction pred4 = new Prediction(
                gt4, null, 1.0, null);
        Prediction pred5 = new Prediction(
                gt5, null, 0.0, null);
        List<Prediction> preds1 = Lists.newArrayList(pred1, pred2, pred3, pred4, pred5);
        auc.add(gts1, preds1);
        pred1 = new Prediction(gt1, null, 5.0, null);
        pred2 = new Prediction(gt4, null, 2.8, null);
        pred3 = new Prediction(gt2, null, 2.3, null);
        pred4 = new Prediction(gt5, null, 1.5, null);
        pred5 = new Prediction(gt3, null, 0.2, null);
        List<Prediction> preds2 = Lists.newArrayList(pred1, pred2, pred3, pred4, pred5);
        List<ObjectNode> gts2 = Lists.newArrayList(gt1, gt4, gt2, gt5, gt3);
        auc.add(gts2, preds2);
        MetricResult results = auc.getResults();
        assertEquals(true, results.getPass());
        assertEquals(1, results.getValues().size());
        JsonNode result = results.getValues().get(0);
        int support = result.get(ConfigKey.EVALUATOR_METRIC_SUPPORT.get()).asInt();
        double value = result.get(ConfigKey.EVALUATOR_METRIC_VALUE.get()).asDouble();
        assertEquals(2, support);
        assertEquals(0.583, value, 0.001);
    }
}
