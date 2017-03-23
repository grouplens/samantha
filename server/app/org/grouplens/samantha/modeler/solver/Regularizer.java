package org.grouplens.samantha.modeler.solver;

import org.apache.commons.math3.linear.RealVector;

import java.util.List;

public interface Regularizer {
    double getValue(double var);
    double getGradient(double var);
    RealVector addGradient(RealVector grad, RealVector var, double coef);
    double getObjective(double coef, RealVector var);
    double getObjective(double coef, List<RealVector> vars);
}
