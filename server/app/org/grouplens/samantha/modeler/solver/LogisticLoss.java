package org.grouplens.samantha.modeler.solver;

import java.util.List;

public class LogisticLoss implements ObjectiveFunction {
    private static final long serialVersionUID = 1L;

    public LogisticLoss() { }

    static public double sigmoid(double y) {
        if (y < -30.0) {
            return 0.001;
        } else if (y > 30.0) {
            return 0.999;
        } else {
            return 1.0 / (1.0 + Math.exp(-y));
        }
    }

    public double wrapOutput(double output) {
        return sigmoid(output);
    }

    public List<StochasticOracle> wrapOracle(List<StochasticOracle> oracles) {
        for (StochasticOracle orc : oracles) {
            double label = orc.getLabel();
            double weight = orc.getWeight();
            double modelOutput = orc.getModelOutput();
            orc.setObjVal(weight * (Math.log(1.0 + Math.exp(modelOutput)) - label * modelOutput));
            orc.setGradient(weight * (1.0 / (1.0 + Math.exp(-modelOutput)) - label));
        }
        return oracles;
    }
}
