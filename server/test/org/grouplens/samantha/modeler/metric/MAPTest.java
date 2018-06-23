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

public class MAPTest {

    @Test
    public void testMAP() {
        MAP map = new MAP(
                Lists.newArrayList(1, 3),
                Lists.newArrayList("item"),
                Lists.newArrayList("item"),
                null, null, 0.0, 0.0);
        ObjectNode gt1 = Json.newObject().put("item", 1);
        ObjectNode gt2 = Json.newObject().put("item", 3);
        ObjectNode ot1 = Json.newObject().put("item", 5);
        List<ObjectNode> gts1 = Lists.newArrayList(gt1, gt2);
        Prediction rec1 = new Prediction(
                ot1, null, 3.0, null);
        Prediction rec2 = new Prediction(
                gt1, null, 2.0, null);
        Prediction rec3 = new Prediction(
                gt2, null, 1.0, null);
        List<Prediction> recs1 = Lists.newArrayList(rec1, rec2, rec3);
        map.add(gts1, recs1);
        rec1 = new Prediction(gt1, null, 3.0, null);
        rec2 = new Prediction(ot1, null, 2.0, null);
        rec3 = new Prediction(gt2, null, 1.0, null);
        List<Prediction> recs2 = Lists.newArrayList(rec1, rec2, rec3);
        map.add(gts1, recs2);
        MetricResult results = map.getResults();
        assertEquals(true, results.getPass());
        assertEquals(2, results.getValues().size());
        for (JsonNode result : results.getValues()) {
            int n = result.get(ConfigKey.EVALUATOR_METRIC_PARA.get()).get("N").asInt();
            double value = result.get(ConfigKey.EVALUATOR_METRIC_VALUE.get()).asDouble();
            assertTrue(n == 1 || n == 3);
            if (n == 1) {
                assertEquals(0.500, value, 0.001);
            } else if (n == 3) {
                assertEquals(0.708, value, 0.001);
            }
        }
    }
}
