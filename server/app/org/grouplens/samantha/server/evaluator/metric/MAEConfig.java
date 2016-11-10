package org.grouplens.samantha.server.evaluator.metric;

import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class MAEConfig implements MetricConfig {
    final private String labelName;

    private MAEConfig(String labelName) {
        this.labelName = labelName;
    }

    public static MetricConfig getMetricConfig(Configuration metricConfig,
                                        Injector injector) {
        String labelName = metricConfig.getString("labelName");
        return new MAEConfig(labelName);
    }

    public Metric getMetric(RequestContext requestContext) {
        return new MAE(labelName);
    }
}
