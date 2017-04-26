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

package org.grouplens.samantha.modeler.tensorflow;

import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.solver.*;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TensorFlowMethod extends AbstractOptimizationMethod implements OnlineOptimizationMethod {
    private static Logger logger = LoggerFactory.getLogger(SolverUtilities.class);

    public TensorFlowMethod(double tol, int maxIter, int minIter) {
        super(tol, maxIter, minIter);
    }

    public double update(LearningModel model, LearningData learningData) {
        ObjectiveFunction objFunc = model.getObjectiveFunction();
        List<LearningInstance> instances;
        double objVal = 0.0;
        int cnt = 0;
        while ((instances = learningData.getLearningInstance()).size() > 0) {
            List<StochasticOracle> oracles = model.getStochasticOracle(instances);
            objFunc.wrapOracle(oracles);
            for (StochasticOracle orc : oracles) {
                objVal += orc.getObjectiveValue();
                if (Double.isNaN(objVal)) {
                    logger.error("Objective value becomes NaN at {}th instance.", cnt);
                    throw new BadRequestException("Got NaN error.");
                }
                for (int i = 0; i < orc.getScalarNames().size(); i++) {
                    String name = orc.getScalarNames().get(i);
                    int idx = orc.getScalarIndexes().getInt(i);
                    double grad = orc.getScalarGrads().getDouble(i);
                    model.setScalarVarByNameIndex(name, idx, grad);
                }
                for (int i = 0; i < orc.getVectorNames().size(); i++) {
                    String name = orc.getVectorNames().get(i);
                    int idx = orc.getVectorIndexes().getInt(i);
                    RealVector grad = orc.getVectorGrads().get(i);
                    model.setVectorVarByNameIndex(name, idx, grad);
                }
                cnt++;
                if (cnt % 100000 == 0) {
                    logger.info("Updated the model using {} instances.", cnt);
                }
            }
        }
        return objVal;
    }
}
