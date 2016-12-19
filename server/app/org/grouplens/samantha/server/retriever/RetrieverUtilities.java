package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;

import java.util.Comparator;
import java.util.List;

import static org.grouplens.samantha.modeler.tree.SortingUtilities.compareValues;

public class RetrieverUtilities {

    private RetrieverUtilities() {}

    static public Ordering<ObjectNode> jsonFieldOrdering(String field) {
        return new Ordering<ObjectNode>() {
            private String orderField = field;
            @Override
            public int compare(ObjectNode left, ObjectNode right) {
                if (left.has(orderField)) {
                    double leftValue = left.get(orderField).asDouble();
                    double rightValue = right.get(orderField).asDouble();
                    return compareValues(leftValue, rightValue);
                } else {
                    return 0;
                }
            }
        };
    }

    static public Comparator<ObjectNode> jsonStringFieldsComparator(List<String> fields) {
        return new Comparator<ObjectNode>() {
            private List<String> orderFields = fields;
            @Override
            public int compare(ObjectNode left, ObjectNode right) {
                for (int i=0; i<orderFields.size(); i++) {
                    String field = orderFields.get(i);
                    String leftVal = left.get(field).asText();
                    String rightVal= right.get(field).asText();
                    if (!leftVal.equals(rightVal)) {
                        return leftVal.compareTo(rightVal);
                    }
                }
                return 0;
            }
        };
    }
}
