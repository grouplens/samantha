package org.grouplens.samantha.modeler.solver;

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

    public double getObjectiveValue(double wrappedOutput, double label, double weight) {
        return weight * ((label - 1) * Math.log(1 - wrappedOutput) - label * Math.log(wrappedOutput));
    }

    public double getGradient(double wrappedOutput, double label, double weight) {
        return weight * (wrappedOutput - label);
    }

    public void wrapOracle(StochasticOracle orc) {
        orc.objVal = orc.insWeight * (Math.log(1.0 + Math.exp(orc.modelOutput)) - orc.insLabel * orc.modelOutput);
        double err = 1.0 / (1.0 + Math.exp(-orc.modelOutput)) - orc.insLabel;
        if (orc.insWeight != 1.0) {
            err *= orc.insWeight;
        }
        orc.gradient = err;
        for (int i=0; i<orc.scalarGrads.size(); i++) {
            orc.scalarGrads.set(i, orc.scalarGrads.getDouble(i) * err);
        }
        for (int i=0; i<orc.vectorGrads.size(); i++) {
            orc.vectorGrads.get(i).mapMultiplyToSelf(err);
        }
    }
}
