package org.grouplens.samantha.server.evaluator.metric;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public class MetricResult {
    @JsonProperty
    final List<ObjectNode> values;
    @JsonProperty
    final boolean pass;

    MetricResult(List<ObjectNode> values, boolean pass) {
        this.values = values;
        this.pass = pass;
    }

    public List<ObjectNode> getValues() {
        return values;
    }

    public boolean getPass() {
        return pass;
    }
}
