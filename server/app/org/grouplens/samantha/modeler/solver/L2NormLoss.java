package org.grouplens.samantha.modeler.solver;

import java.util.List;

public class L2NormLoss implements ObjectiveFunction {
    private static final long serialVersionUID = 1L;

    public L2NormLoss() {
    }

    public List<StochasticOracle> wrapOracle(List<StochasticOracle> oracles) {
        for (StochasticOracle orc : oracles) {
            double modelOutput = orc.getModelOutput();
            double label = orc.getLabel();
            double weight = orc.getWeight();
            double err = modelOutput - label;
            orc.setObjVal(err * err * weight);
            orc.setGradient(err * weight);
        }
        return oracles;
    }
}