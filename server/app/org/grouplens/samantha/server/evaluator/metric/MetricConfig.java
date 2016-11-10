package org.grouplens.samantha.server.evaluator.metric;

import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public interface MetricConfig {
    static MetricConfig getMetricConfig(Configuration metricConfig,
                                              Injector injector) {return null;}
    Metric getMetric(RequestContext requestContext);
}
