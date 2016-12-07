package org.grouplens.samantha.server.evaluator.metric;

import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class NDCGConfig implements MetricConfig {
    final List<Integer> N;
    final List<String> itemKeys;
    final String relevanceKey;
    final double minValue;

    private NDCGConfig(List<Integer> N, List<String> itemKeys, String relevanceKey, double minValue) {
        this.N = N;
        this.itemKeys = itemKeys;
        this.relevanceKey = relevanceKey;
        this.minValue = minValue;
    }

    public static MetricConfig getMetricConfig(Configuration metricConfig,
                                               Injector injector) {
        double minValue = 0.0;
        if (metricConfig.asMap().containsKey("minValue")) {
            minValue = metricConfig.getDouble("minValue");
        }
        return new NDCGConfig(metricConfig.getIntList("N"),
                metricConfig.getStringList("itemKeys"),
                metricConfig.getString("relevanceKey"), minValue);
    }

    public Metric getMetric(RequestContext requestContext) {
        return new NDCG(this);
    }
}
