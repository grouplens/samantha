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

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerminationCriterion {
    final private static Logger logger = LoggerFactory.getLogger(TerminationCriterion.class);
    final private int maxIter;
    final private int minIter;
    final private double tol;
    final private DoubleList objHistory;
    private int curIter;

    public TerminationCriterion(double tol, int maxIter, int minIter) {
        this.maxIter = maxIter;
        this.tol = tol;
        this.minIter = minIter;
        curIter = 0;
        objHistory = new DoubleArrayList();
    }

    public void addIteration(double objVal) {
        curIter++;
        objHistory.add(objVal);
        logger.info("Iteration {}: objective value is {}", curIter, objVal);
    }

    public void addIteration(String step, double objVal) {
        curIter++;
        objHistory.add(objVal);
        logger.info("{}, Iteration {}: objective value is {}", step, curIter, objVal);
    }

    public boolean keepIterate() {
        if (curIter < minIter) {
            return true;
        } else if (curIter >= maxIter) {
            return false;
        } else if (objHistory.getDouble(curIter - 2) - objHistory.getDouble(curIter - 1) <= tol) {
            return false;
        } else {
            return true;
        }
    }
}
