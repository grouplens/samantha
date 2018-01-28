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

package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.grouplens.samantha.server.indexer.SQLBasedIndexer;

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

    static public Ordering<Object2DoubleMap.Entry<String>> object2DoubleEntryOrdering() {
        return new Ordering<Object2DoubleMap.Entry<String>>() {
            @Override
            public int compare(Object2DoubleMap.Entry<String> left, Object2DoubleMap.Entry<String> right) {
                return compareValues(left.getDoubleValue(), right.getDoubleValue());
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

    static public Comparator<ObjectNode> jsonTypedFieldsComparator(List<String> fields, List<String> types) {
        return new Comparator<ObjectNode>() {
            private List<String> orderFields = fields;
            private List<String> fieldTypes = types;
            @Override
            public int compare(ObjectNode left, ObjectNode right) {
                for (int i=0; i<orderFields.size(); i++) {
                    String field = orderFields.get(i);
                    int ret = SQLBasedIndexer.BasicType.valueOf(fieldTypes.get(i)).compareValue(
                            field, left, right);
                    if (ret != 0) {
                        return ret;
                    }
                }
                return 0;
            }
        };
    }
}
