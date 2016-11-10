package org.grouplens.samantha.modeler.solver;

public class HingeLoss implements ObjectiveFunction {
    private static final long serialVersionUID = 1L;

    public HingeLoss() { }

    public double wrapOutput(double output) {
        return output;
    }

    private double getLoss(double wrappedOutput, double label, double weight) {
        return (1 - wrappedOutput * label) * weight;
    }

    public double getObjectiveValue(double wrappedOutput, double label, double weight) {
        if (label == 0) {
            label = -1;
        }
        double loss = getLoss(wrappedOutput, label, weight);
        return (loss < 0) ? 0 : loss;
    }

    public double getGradient(double wrappedOutput, double label, double weight) {
        if (label == 0) {
            label = -1;
        }
        double loss = getLoss(wrappedOutput, label, weight);
        return (loss == 0) ? 0 : -label;
    }

    public void wrapOracle(StochasticOracle orc) {
        double label = orc.insLabel;
        if (label == 0) {
            label = -1;
        }
        double loss = getLoss(orc.modelOutput, label, orc.insWeight);
        orc.objVal = (loss < 0) ? 0 : loss;
        orc.gradient = (loss == 0) ? 0 : -label;
        for (int i=0; i<orc.scalarGrads.size(); i++) {
            orc.scalarGrads.set(i, orc.scalarGrads.getDouble(i) * orc.gradient);
        }
        for (int i=0; i<orc.vectorGrads.size(); i++) {
            orc.vectorGrads.get(i).mapMultiplyToSelf(orc.gradient);
        }
    }
}
