package org.grouplens.samantha.server.evaluator.metric;

import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class AUCConfig implements MetricConfig {
    final private AUC.AUCType aucType;
    final private String labelName;
    final private double threshold;

    private AUCConfig(AUC.AUCType aucType, String labelName, double threshold) {
        this.aucType = aucType;
        this.labelName = labelName;
        this.threshold = threshold;
    }

    public static MetricConfig getMetricConfig(Configuration metricConfig,
                                               Injector injector) {
        String aucType = metricConfig.getString("aucType");
        double threshold = 0.5;
        if (metricConfig.asMap().containsKey("threshold")) {
            threshold = metricConfig.getDouble("threshold");
        }
        return new AUCConfig(AUC.AUCType.valueOf(aucType), metricConfig.getString("labelName"), threshold);
    }

    public Metric getMetric(RequestContext requestContext) {
        return new AUC(labelName, aucType, threshold);
    }
}
