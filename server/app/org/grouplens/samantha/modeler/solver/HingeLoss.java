package org.grouplens.samantha.modeler.solver;

import java.util.List;

public class HingeLoss implements ObjectiveFunction {
    private static final long serialVersionUID = 1L;

    public HingeLoss() { }

    private double getLoss(double wrappedOutput, double label, double weight) {
        return (1 - wrappedOutput * label) * weight;
    }

    public List<StochasticOracle> wrapOracle(List<StochasticOracle> oracles) {
        for (StochasticOracle orc : oracles) {
            double label = orc.getLabel();
            if (label == 0) {
                label = -1;
            }
            double loss = getLoss(orc.getModelOutput(), label, orc.getWeight());
            orc.setObjVal((loss < 0) ? 0 : loss);
            orc.setGradient((loss == 0) ? 0 : -label);
        }
        return oracles;
    }
}
