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

}
