package org.grouplens.samantha.server.evaluator.metric;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.config.ConfigKey;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

public class RMSE implements Metric {
    private final String labelName;
    private final double maxValue;
    private double errorSquared = 0;
    private long n = 0;

    public RMSE(String labelName, double maxValue) {
        this.labelName = labelName;
        this.maxValue = maxValue;
    }

    public void add(List<ObjectNode> groundTruth, List<Prediction> predictions) {
        int num = groundTruth.size();
        n += num;
        for (int i=0; i<num; i++) {
            double err = groundTruth.get(i).get(labelName).asDouble()
                    - predictions.get(i).getScore();
            errorSquared += err * err;
        }
    }

    public MetricResult getResults() {
        ObjectNode result = Json.newObject();
        result.put(ConfigKey.EVALUATOR_METRIC_NAME.get(), "RMSE");
        ObjectNode para = Json.newObject();
        para.put("N", n);
        result.set(ConfigKey.EVALUATOR_METRIC_PARA.get(), para);
        double value = 0.0;
        if (n > 0) {
            value = Math.sqrt(errorSquared / n);
        }
        result.put(ConfigKey.EVALUATOR_METRIC_VALUE.get(), value);
        List<ObjectNode> results = new ArrayList<>(1);
        results.add(result);
        boolean pass = true;
        if (value > maxValue) {
            pass = false;
        }
        return new MetricResult(results, pass);
    }
}
