package org.grouplens.samantha.modeler.solver;

import com.google.inject.ImplementedBy;

import java.io.Serializable;
import java.util.List;

@ImplementedBy(L2NormLoss.class)
public interface ObjectiveFunction extends Serializable {
    List<StochasticOracle> wrapOracle(List<StochasticOracle> oracles);
    default double wrapOutput(double modelOutput) {
        return modelOutput;
    }
}
