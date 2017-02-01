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

package org.grouplens.samantha.server.evaluator.metric;

import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class AUCConfig implements MetricConfig {
    final private AUC.AUCType aucType;
    final private String labelName;
    final private double threshold;
    final private double minValue;

    private AUCConfig(AUC.AUCType aucType, String labelName, double threshold, double minValue) {
        this.aucType = aucType;
        this.labelName = labelName;
        this.threshold = threshold;
        this.minValue = minValue;
    }

    public static MetricConfig getMetricConfig(Configuration metricConfig,
                                               Injector injector) {
        String aucType = metricConfig.getString("aucType");
        double threshold = 0.5;
        if (metricConfig.asMap().containsKey("threshold")) {
            threshold = metricConfig.getDouble("threshold");
        }
        double minValue = 0.5;
        if (metricConfig.asMap().containsKey("minValue")) {
            minValue = metricConfig.getDouble("minValue");
        }
        return new AUCConfig(AUC.AUCType.valueOf(aucType), metricConfig.getString("labelName"),
                threshold, minValue);
    }

    public Metric getMetric(RequestContext requestContext) {
        return new AUC(labelName, aucType, threshold, minValue);
    }
}
