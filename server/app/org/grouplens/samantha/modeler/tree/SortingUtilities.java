package org.grouplens.samantha.modeler.tree;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Ordering;

import java.util.Comparator;

public class SortingUtilities {

    static public int compareValues(double leftScore, double rightScore) {
        if (leftScore > rightScore) {
            return 1;
        } else if (leftScore < rightScore) {
            return -1;
        } else {
            return 0;
        }
    }

    static public Comparator<double[]> pairDoubleSecondComparator() {
        return new Comparator<double[]>() {
            @Override
            public int compare(double[] o1, double[] o2) {
                return compareValues(o1[1], o2[1]);
            }
        };
    }

    static public Comparator<double[]> pairDoubleSecondReverseComparator() {
        return new Comparator<double[]>() {
            @Override
            public int compare(double[] o1, double[] o2) {
                return compareValues(o2[1], o1[1]);
            }
        };
    }

    static public Comparator<double[]> pairDoubleFirstComparator() {
        return new Comparator<double[]>() {
            @Override
            public int compare(double[] o1, double[] o2) {
                return compareValues(o1[0], o2[0]);
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

    static public Comparator<JsonNode> jsonFieldReverseComparator(String field) {
        return new Comparator<JsonNode>() {
            @Override
            public int compare(JsonNode o1, JsonNode o2) {
                return compareValues(o2.get(field).asDouble(), o1.get(field).asDouble());
            }
        };
    }
}
