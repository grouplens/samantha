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
