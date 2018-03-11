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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.config.ConfigKey;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

public class RMSE implements Metric {
    private final String labelName;
    private final double maxValue;
    private double errorSquared = 0;
    private long n = 0;

    public RMSE(String labelName, double maxValue) {
        this.labelName = labelName;
        this.maxValue = maxValue;
    }

    public void add(List<ObjectNode> groundTruth, List<Prediction> predictions) {
        int num = groundTruth.size();
        n += num;
        for (int i=0; i<num; i++) {
            double err = groundTruth.get(i).get(labelName).asDouble()
                    - predictions.get(i).getScore();
            errorSquared += err * err;
        }
    }

    public MetricResult getResults() {
        ObjectNode result = Json.newObject();
        result.put(ConfigKey.EVALUATOR_METRIC_NAME.get(), "RMSE");
        ObjectNode para = Json.newObject();
        para.put("N", n);
        result.set(ConfigKey.EVALUATOR_METRIC_PARA.get(), para);
        double value = 0.0;
        if (n > 0) {
            value = Math.sqrt(errorSquared / n);
        }
        result.put(ConfigKey.EVALUATOR_METRIC_VALUE.get(), value);
        List<ObjectNode> results = new ArrayList<>(1);
        results.add(result);
        boolean pass = true;
        if (value > maxValue) {
            pass = false;
        }
        return new MetricResult(results, pass);
    }
}
