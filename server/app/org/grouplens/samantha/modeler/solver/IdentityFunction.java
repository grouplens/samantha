package org.grouplens.samantha.modeler.solver;

import java.util.List;

public class IdentityFunction implements ObjectiveFunction {
    private static final long serialVersionUID = 1L;

    public IdentityFunction() {}

    public List<StochasticOracle> wrapOracle(List<StochasticOracle> oracles) {
        for (StochasticOracle orc : oracles) {
            double weight = orc.getWeight();
            orc.setObjVal(orc.getModelOutput() * weight);
            orc.setGradient(weight);
        }
        return oracles;
    }
}
