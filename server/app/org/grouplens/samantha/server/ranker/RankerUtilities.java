/*
 * Copyright (c) [2016-2018] [University of Minnesota]
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

package org.grouplens.samantha.server.ranker;

import com.google.common.collect.Ordering;
import org.grouplens.samantha.server.predictor.Prediction;

import static org.grouplens.samantha.modeler.tree.SortingUtilities.compareValues;

public class RankerUtilities {
    private RankerUtilities() {}

    static public int defaultPageSize = 24;

    static public Ordering<Prediction> scoredResultScoreOrdering() {
        return new Ordering<Prediction>() {
            @Override
            public int compare(Prediction left, Prediction right) {
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
            public int compare(Prediction left, Prediction right) {
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
