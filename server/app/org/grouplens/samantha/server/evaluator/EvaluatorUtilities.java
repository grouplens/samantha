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

package org.grouplens.samantha.server.evaluator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.evaluator.metric.Metric;
import org.grouplens.samantha.server.evaluator.metric.MetricConfig;
import org.grouplens.samantha.server.evaluator.metric.MetricResult;
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

    static public List<MetricResult> indexMetrics(Configuration config,
                                                  RequestContext requestContext,
                                                  List<Metric> metrics,
                                                  List<Indexer> indexers) {
        List<MetricResult> all = new ArrayList<>();
        for (Metric metric : metrics) {
            MetricResult result = metric.getResults();
            for (ObjectNode value : result.getValues()) {
                value.put(ConfigKey.EVALUATOR_ENGINE_NAME.get(),
                        requestContext.getEngineName());
                value.set(ConfigKey.ENGINE_COMPONENT_CONFIG.get(), Json.toJson(config.asMap()));
                value.put(ConfigKey.REQUEST_CONTEXT.get(), requestContext.getRequestBody().toString());
            }
            all.add(result);
            for (Indexer indexer : indexers) {
                indexer.index(Json.toJson(result), requestContext);
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
