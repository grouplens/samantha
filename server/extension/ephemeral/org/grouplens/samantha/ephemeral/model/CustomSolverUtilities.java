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

package org.grouplens.samantha.ephemeral.model;

import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.solver.*;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CustomSolverUtilities {

    private CustomSolverUtilities() {}

    private static Logger logger = LoggerFactory.getLogger(CustomSolverUtilities.class);


    public static double stochasticGradientDescentUpdate(LearningModel model, ObjectiveFunction objFunc,
                                                         LearningData learningData, L2Regularizer l2term,
                                                         double l2coef, double lr, boolean nonnegative) {
        int cnt = 0;
        double objVal = 0.0;
        List<LearningInstance> instances;
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
                    double var = model.getScalarVarByNameIndex(name, idx);
                    var = var - lr * (grad + l2coef * l2term.getGradient(var));
                    if (nonnegative) { var = Math.max(0.0, var); }
                    model.setScalarVarByNameIndex(name, idx, var);
                }
                for (int i = 0; i < orc.getVectorNames().size(); i++) {
                    String name = orc.getVectorNames().get(i);
                    int idx = orc.getVectorIndexes().getInt(i);
                    RealVector grad = orc.getVectorGrads().get(i);
                    RealVector var = model.getVectorVarByNameIndex(name, idx); // returns a copy
                    var = var.combineToSelf(1.0, -lr, l2term.addGradient(grad, var, l2coef));
                    if (nonnegative) { var = var.mapToSelf(x -> Math.max(0.0, x)); }
                    model.setVectorVarByNameIndex(name, idx, var);
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
