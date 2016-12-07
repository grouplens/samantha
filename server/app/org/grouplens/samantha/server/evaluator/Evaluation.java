package org.grouplens.samantha.server.evaluator;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.grouplens.samantha.server.evaluator.metric.MetricResult;

import java.util.List;

public class Evaluation {
    @JsonProperty
    private final List<MetricResult> metrics;
    @JsonProperty
    private final boolean pass;

    Evaluation(List<MetricResult> metrics, boolean pass) {
        this.metrics = metrics;
        this.pass = pass;
    }

    Evaluation(List<MetricResult> metrics) {
        this.metrics = metrics;
        boolean pass = true;
        for (MetricResult metric : metrics) {
            if (!metric.getPass()) {
                pass = false;
            }
        }
        this.pass = pass;
    }

    public boolean getPass() {
        return pass;
    }
}
