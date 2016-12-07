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
