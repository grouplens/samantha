package org.grouplens.samantha.modeler.solver;

import com.google.inject.ImplementedBy;

import java.io.Serializable;

@ImplementedBy(L2NormLoss.class)
public interface ObjectiveFunction extends Serializable {
    void wrapOracle(StochasticOracle orc);
    double wrapOutput(double modelOutput);
    double getObjectiveValue(double wrappedOutput, double label, double weight);
    double getGradient(double wrappedOutput, double label, double weight);
}
