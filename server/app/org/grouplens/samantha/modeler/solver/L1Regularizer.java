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

import org.apache.commons.math3.linear.RealVector;

import java.util.List;

public class L1Regularizer implements Regularizer {
    public L1Regularizer() {}

    public double getValue(double var) {
        return Math.abs(var);
    }

    public double getGradient(double var) {
        if (var > 0) {
            return 1;
        } else if (var < 0) {
            return -1;
        } else {
            return 0; //sub gradient: any one in [-1, 1]
        }
    }

    public double getObjective(double l1coef, RealVector var) {
        double l1norm = var.getL1Norm();
        return l1coef * l1norm;
    }

    public double getObjective(double l1coef, List<RealVector> vars) {
        double objVal = 0.0;
        for (RealVector realVector : vars) {
            double l1norm = realVector.getL1Norm();
            objVal += l1norm;
        }
        return objVal * l1coef;
    }
}
