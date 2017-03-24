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

public class SGDRunnable implements ObjectiveRunnable {
    private final LearningModel learningModel;
    private final LearningData learningData;
    private final double l2coef;
    private final double lr;
    private double objVal = 0.0;

    SGDRunnable(LearningModel learningModel, LearningData learningData, double l2coef, double lr) {
        this.learningData = learningData;
        this.learningModel = learningModel;
        this.l2coef = l2coef;
        this.lr = lr;
    }

    public void run() {
        L2Regularizer l2term = new L2Regularizer();
        ObjectiveFunction objFunc = learningModel.getObjectiveFunction();
        objVal += SolverUtilities.stochasticGradientDescentUpdate(learningModel, objFunc,
                learningData, l2term, l2coef, lr);
    }

    public double getObjVal() {
        return objVal;
    }
}
