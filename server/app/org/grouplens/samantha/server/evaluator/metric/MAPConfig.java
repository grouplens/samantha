package org.grouplens.samantha.server.evaluator.metric;

import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class MAPConfig implements MetricConfig {
    final List<Integer> N;
    final List<String> itemKeys;
    final String relevanceKey;
    final double threshold;
    final double minValue;

    private MAPConfig(List<Integer> N, List<String> itemKeys, String relevanceKey,
                      double threshold, double minValue) {
        this.N = N;
        this.itemKeys = itemKeys;
        this.relevanceKey = relevanceKey;
        this.threshold = threshold;
        this.minValue = minValue;
    }

    public static MetricConfig getMetricConfig(Configuration metricConfig,
                                               Injector injector) {
        double threshold = 0.0;
        if (metricConfig.asMap().containsKey("threshold")) {
            threshold = metricConfig.getDouble("threshold");
        }
        double minValue = 0.0;
        if (metricConfig.asMap().containsKey("minValue")) {
            minValue = metricConfig.getDouble("minValue");
        }
        return new MAPConfig(metricConfig.getIntList("N"),
                metricConfig.getStringList("itemKeys"), metricConfig.getString("relevanceKey"),
                threshold, minValue);
    }

    public Metric getMetric(RequestContext requestContext) {
        return new MAP(this);
    }
}
