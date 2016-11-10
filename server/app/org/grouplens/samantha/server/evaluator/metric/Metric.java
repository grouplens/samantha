package org.grouplens.samantha.server.evaluator.metric;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.predictor.Prediction;

import java.util.List;

public interface Metric {
    void add(List<ObjectNode> groundTruth, List<Prediction> results);
    List<ObjectNode> getValues();
}
