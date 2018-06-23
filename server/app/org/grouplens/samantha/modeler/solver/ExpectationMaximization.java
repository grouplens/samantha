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

import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.model.LatentLearningModel;
import org.grouplens.samantha.modeler.model.LearningModel;

import java.util.List;

public class ExpectationMaximization extends AbstractOptimizationMethod {
    private OptimizationMethod method;
    
    public ExpectationMaximization() {
        super(1.0, 50, 2);
        method = new StochasticGradientDescent(3, 2, 0.0, 0.01, 10);
    }

    public ExpectationMaximization(double tol, int maxIter, int minIter,
                                   double subTol, int subMaxIter, int subMinIter,
                                   double l2coef, double learningRate) {
        super(tol, maxIter, minIter);
        method = new StochasticGradientDescent(subMaxIter, subMinIter, l2coef, learningRate, subTol);
    }

    public double minimize(LearningModel learningModel, LearningData learningData, LearningData validData) {
        LatentLearningModel model = (LatentLearningModel)learningModel;
        TerminationCriterion termCrit = new TerminationCriterion(tol, maxIter, minIter);
        double objVal = 0;
        while (termCrit.keepIterate()) {
            objVal = 0;
            learningData.startNewIteration();
            List<LearningInstance> instances;
            while ((instances = learningData.getLearningInstance()).size() > 0) {
                for (LearningInstance ins : instances) {
                    objVal += model.expectation(ins);
                }
            }
            termCrit.addIteration(objVal);
            LearningModel subModel = model.maximization();
            if (subModel != null) {
                method.minimize(subModel, learningData, validData);
            }
        }
        return objVal;
    }
}
