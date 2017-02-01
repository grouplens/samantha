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

package org.grouplens.samantha.modeler.solver;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.apache.commons.math3.linear.MatrixUtils;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.Random;

public class RandomInitializer {
    final private double multi;
    final private double subtract;
    final private Random rand;

    public RandomInitializer() {
        this.multi = 0.1;
        this.subtract = 0.5;
        this.rand = new Random();
    }

    public RandomInitializer(long seed, double multiplier, double subtract) {
        this.multi = multiplier;
        this.subtract = subtract;
        this.rand = new Random(seed);
    }

    public double randInitValue() {
        return (rand.nextDouble() - subtract) * multi;
    }

    public void randInitVector(RealVector vec, boolean normalize) {
        int len = vec.getDimension();
        double sum = 0.0;
        for (int i=0; i<len; i++) {
            double val;
            if (normalize) {
                val = rand.nextDouble();
            } else {
                val = (rand.nextDouble() - subtract) * multi;
            }
            vec.setEntry(i, val);
            if (normalize) {
                sum += val;
            }
        }
        if (normalize) {
            vec.mapDivideToSelf(sum);
        }
    }

    public void randInitDoubleList(DoubleList doubleList, boolean normalize) {
        int size = doubleList.size();
        double sum = 0.0;
        for (int i=0; i<size; i++) {
            double val;
            if (normalize) {
                val = rand.nextDouble();
            } else {
                val = (rand.nextDouble() - subtract) * multi;
            }
            doubleList.set(i, val);
            if (normalize) {
                sum += val;
            }
        }
        if (normalize) {
            for (int i=0; i<size; i++) {
                doubleList.set(i, doubleList.getDouble(i) / sum);
            }
        }
    }

    public void randInitMatrix(RealMatrix mat, boolean normalize) {
        int len = mat.getRowDimension();
        RealVector vec = MatrixUtils.createRealVector(new double[mat.getColumnDimension()]);
        for (int i=0; i<len; i++) {
            randInitVector(vec, normalize);
            mat.setRowVector(i, vec);
        }
    }
}
