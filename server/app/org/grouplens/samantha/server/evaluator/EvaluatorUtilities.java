package org.grouplens.samantha.server.evaluator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.evaluator.metric.Metric;
import org.grouplens.samantha.server.evaluator.metric.MetricConfig;
import org.grouplens.samantha.server.exception.ConfigurationException;
import org.grouplens.samantha.server.indexer.Indexer;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class EvaluatorUtilities {
    private EvaluatorUtilities() {}

    static public List<ObjectNode> indexMetrics(String type, Configuration config,
                                                RequestContext requestContext,
                                                List<Metric> metrics,
                                                List<Indexer> indexers) {
        List<ObjectNode> all = new ArrayList<>();
        for (Metric metric : metrics) {
            List<ObjectNode> results = metric.getValues();
            for (ObjectNode result : results) {
                result.put(ConfigKey.EVALUATOR_ENGINE_NAME.get(),
                        requestContext.getEngineName());
                result.set(ConfigKey.ENGINE_COMPONENT_CONFIG.get(), Json.toJson(config.asMap()));
                result.put(ConfigKey.REQUEST_CONTEXT.get(), requestContext.getRequestBody().toString());
            }
            all.addAll(results);
            for (Indexer indexer : indexers) {
                indexer.index(type, Json.toJson(results), requestContext);
            }
        }
        return all;
    }

    static public List<MetricConfig> getMetricConfigs(List<Configuration> configList,
                                                      Injector injector) {
        List<MetricConfig> metricConfigs = new ArrayList<>();
        try {
            for (Configuration config : configList) {
                String metricConfigClass = config.getString(ConfigKey
                        .METRIC_CONFIG_CLASS.get());
                Method method = Class.forName(metricConfigClass)
                        .getMethod("getMetricConfig", Configuration.class,
                                Injector.class);
                MetricConfig metricConfig = (MetricConfig) method
                        .invoke(null, config, injector);
                metricConfigs.add(metricConfig);
            }
        } catch (ClassNotFoundException | IllegalAccessException |
                NoSuchMethodException | InvocationTargetException e) {
            throw new ConfigurationException(e);

        }
        return metricConfigs;
    }
}
