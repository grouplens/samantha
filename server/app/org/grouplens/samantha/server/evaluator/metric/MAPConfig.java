package org.grouplens.samantha.server.evaluator.metric;

import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class MAPConfig implements MetricConfig {
    final List<Integer> N;
    final List<String> itemKeys;
    final String ratingKey;
    final double leastRating;

    private MAPConfig(List<Integer> N, List<String> itemKeys, String ratingKey, double leastRating) {
        this.N = N;
        this.itemKeys = itemKeys;
        this.ratingKey = ratingKey;
        this.leastRating = leastRating;
    }

    public static MetricConfig getMetricConfig(Configuration metricConfig,
                                               Injector injector) {
        double leastRating = 0.0;
        if (metricConfig.asMap().containsKey("leastRating")) {
            leastRating = metricConfig.getDouble("leastRating");
        }
        return new MAPConfig(metricConfig.getIntList("N"),
                metricConfig.getStringList("itemKeys"), metricConfig.getString("ratingKey"), leastRating);
    }

    public Metric getMetric(RequestContext requestContext) {
        return new MAP(this);
    }
}
