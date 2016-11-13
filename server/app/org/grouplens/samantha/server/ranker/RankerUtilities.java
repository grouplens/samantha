package org.grouplens.samantha.server.ranker;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Ordering;
import org.grouplens.samantha.server.predictor.Prediction;

import javax.annotation.Nullable;

import java.util.Comparator;

import static org.grouplens.samantha.modeler.tree.SortingUtilities.compareValues;

public class RankerUtilities {
    private RankerUtilities() {}

    static public int defaultPageSize = 24;

    static public Ordering<Prediction> scoredResultScoreOrdering() {
        return new Ordering<Prediction>() {
            @Override
            public int compare(@Nullable Prediction left, @Nullable Prediction right) {
                double leftScore = left.getScore();
                double rightScore = right.getScore();
                return compareValues(leftScore, rightScore);
            }
        };
    }


    static public Ordering<Prediction> scoredResultFieldOrdering(String field) {
        return new Ordering<Prediction>() {
            private String orderField = field;
            @Override
            public int compare(@Nullable Prediction left, @Nullable Prediction right) {
                if (left.getEntity().has(orderField)) {
                    double leftValue = left.getEntity().get(orderField).asDouble();
                    double rightValue = right.getEntity().get(orderField).asDouble();
                    return compareValues(leftValue, rightValue);
                } else {
                    return 0;
                }
            }
        };
    }

    static public Ordering<double[]> pairDoubleSecondOrdering() {
        return new Ordering<double[]>() {
            @Override
            public int compare(double[] left, double[] right) {
                return compareValues(left[1], right[1]);
            }
        };
    }

    static public Comparator<JsonNode> jsonFieldComparator(String field) {
        return new Comparator<JsonNode>() {
            @Override
            public int compare(JsonNode o1, JsonNode o2) {
                return compareValues(o1.get(field).asDouble(), o2.get(field).asDouble());
            }
        };
    }
}
