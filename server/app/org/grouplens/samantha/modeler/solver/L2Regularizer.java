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

package org.grouplens.samantha.modeler.solver;

import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class L2Regularizer implements Regularizer {
    public L2Regularizer() {}

    public double getValue(double var) {
        return var * var;
    }

    public double getGradient(double var) {
        return 2 * var;
    }

    public RealVector addGradient(RealVector grad, RealVector var, double l2coef) {
        return grad.combine(1.0, 2 * l2coef, var);
    }

    public double getObjective(double l2coef, RealVector var) {
        double l2norm = var.getNorm();
        return l2coef * l2norm * l2norm;
    }

    public double getObjective(double l2coef, List<RealVector> vars) {
        double objVal = 0.0;
        for (RealVector realVector : vars) {
            double l2norm = realVector.getNorm();
            objVal += l2norm * l2norm;
        }
        return objVal * l2coef;
    }
}
