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

package org.grouplens.samantha.modeler.tree;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.List;

import static java.lang.Math.abs;

public class MeanSquaredError implements RegressionCriterion {
    private static final long serialVersionUID = 1L;
    transient private final IntSet relevant = new IntOpenHashSet();
    transient private double sumWeight = 0.0;

    public MeanSquaredError() {

    }

    public void add(IntList idxList, List<double[]> resps) {
        for (int i=0; i<idxList.size(); i++) {
            add(idxList.getInt(i), resps);
        }
    }

    public void add(int idx, List<double[]> resps) {
        relevant.add(idx);
    }

    public void remove(IntList idxList, List<double[]> resps) {
        for (int i=0; i<idxList.size(); i++) {
            remove(idxList.getInt(i), resps);
        }
    }

    public void remove(int idx, List<double[]> resps) {
        relevant.remove(idx);
    }

    public double getValue(List<double[]> resps) {
        double sse = 0.0;
        double mean = 0.0;
        IntIterator iter = relevant.iterator();
        while (iter.hasNext()) {
            int idx = iter.nextInt();
            mean += resps.get(idx)[0];
        }
        mean /= relevant.size();
        iter = relevant.iterator();
        sumWeight = 0.0;
        while (iter.hasNext()) {
            int idx = iter.nextInt();
            double[] resp = resps.get(idx);
            double err = mean - resp[0];
            sse += (resp[1] * err * err);
            sumWeight += resp[1];
        }
        return sse / sumWeight;
    }

    public double getSplittingGain(double beforeValue, SplittingCriterion leftSplit,
                                   SplittingCriterion rightSplit, List<double[]> resps) {
        double leftValue = leftSplit.getValue(resps);
        double rightValue = rightSplit.getValue(resps);
        MeanSquaredError left = (MeanSquaredError) leftSplit;
        MeanSquaredError right = (MeanSquaredError) rightSplit;
        return beforeValue - (leftValue * left.sumWeight + rightValue * right.sumWeight) /
                (left.sumWeight + right.sumWeight);
    }

    public MeanSquaredError create() {
        return new MeanSquaredError();
    }
}
