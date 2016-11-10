package org.grouplens.samantha.server.evaluator.metric;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.config.ConfigKey;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

public class MAE implements Metric {
    private final String labelName;
    private double error = 0;
    private long n = 0;

    public MAE(String labelName) {
        this.labelName = labelName;
    }

    public void add(List<ObjectNode> groundTruth, List<Prediction> predictions) {
        int num = groundTruth.size();
        n += num;
        for (int i=0; i<num; i++) {
            error += Math.abs(groundTruth.get(i).get(labelName).asDouble()
                    - predictions.get(i).getScore());
        }
    }

    public List<ObjectNode> getValues() {
        ObjectNode result = Json.newObject();
        result.put(ConfigKey.EVALUATOR_METRIC_NAME.get(), "MAE");
        ObjectNode para = Json.newObject();
        para.put("N", n);
        result.set(ConfigKey.EVALUATOR_METRIC_PARA.get(), para);
        if (n > 0) {
            result.put(ConfigKey.EVALUATOR_METRIC_VALUE.get(), error / n);
        } else {
            result.put(ConfigKey.EVALUATOR_METRIC_VALUE.get(), 0.0);
        }
        List<ObjectNode> results = new ArrayList<>(1);
        results.add(result);
        return results;
    }
}
