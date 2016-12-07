package org.grouplens.samantha.server.evaluator.metric;

import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class RMSEConfig implements MetricConfig {
    final private String labelName;
    final private double maxValue;

    private RMSEConfig(String labelName, double maxValue) {
        this.labelName = labelName;
        this.maxValue = maxValue;
    }

    public static MetricConfig getMetricConfig(Configuration metricConfig,
                                               Injector injector) {
        String labelName = metricConfig.getString("labelName");
        double maxValue = Double.MAX_VALUE;
        if (metricConfig.asMap().containsKey("maxValue")) {
            maxValue = metricConfig.getDouble("maxValue");
        }
        return new RMSEConfig(labelName, maxValue);
    }

    public Metric getMetric(RequestContext requestContext) {
        return new RMSE(labelName, maxValue);
    }
}
