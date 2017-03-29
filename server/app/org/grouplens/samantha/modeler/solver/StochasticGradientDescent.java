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

import org.grouplens.samantha.modeler.common.LearningData;

public class StochasticGradientDescent extends AbstractOptimizationMethod implements OnlineOptimizationMethod {
    private double l2coef;
    private double lr;

    public StochasticGradientDescent() {
        super(5.0, 50, 2);
        l2coef = 0.0;
        lr = 0.001;
    }

    public StochasticGradientDescent(int maxIter, int minIter, double l2coef, double learningRate, double tol) {
        super(tol, maxIter, minIter);
        this.l2coef = l2coef;
        this.lr = learningRate;
    }

    public double update(LearningModel model, LearningData learningData) {
        L2Regularizer l2term = new L2Regularizer();
        ObjectiveFunction objFunc = model.getObjectiveFunction();
        learningData.startNewIteration();
        double objVal = SolverUtilities.stochasticGradientDescentUpdate(model, objFunc,
                learningData, l2term, l2coef, lr);
        return objVal;
    }
}
