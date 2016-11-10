package org.grouplens.samantha.modeler.solver;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerminationCriterion {
    private static Logger logger = LoggerFactory.getLogger(TerminationCriterion.class);
    private int maxIter;
    private int curIter;
    private double tol;
    private DoubleList objHistory;

    public TerminationCriterion(double tol, int maxIter) {
        this.maxIter = maxIter;
        this.tol = tol;
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
        if (curIter < 2) {
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
