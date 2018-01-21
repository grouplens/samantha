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

package org.grouplens.samantha.modeler.tree;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Ordering;

import java.util.Comparator;
import java.util.Map;

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

    static public Comparator<Map.Entry<String, Double>> mapEntryValueComparator() {
        return new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return compareValues(o1.getValue(), o2.getValue());
            }
        };
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
