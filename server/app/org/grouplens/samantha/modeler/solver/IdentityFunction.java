package org.grouplens.samantha.modeler.solver;

public class IdentityFunction implements ObjectiveFunction {
    private static final long serialVersionUID = 1L;

    public IdentityFunction() {}

    public double wrapOutput(double modelOutput) {
        return modelOutput;
    }

    public double getObjectiveValue(double wrappedOutput, double label, double weight) {
        return wrappedOutput * weight;
    }

    public double getGradient(double wrappedOutput, double label, double weight) {
        return weight;
    }

    public void wrapOracle(StochasticOracle orc) {
        orc.objVal = orc.modelOutput * orc.insWeight;
        if (orc.insWeight != 1.0) {
            for (int i=0; i<orc.scalarGrads.size(); i++) {
                orc.scalarGrads.set(i, orc.scalarGrads.getDouble(i) * orc.insWeight);
            }
            for (int i=0; i<orc.vectorGrads.size(); i++) {
                orc.vectorGrads.get(i).mapMultiplyToSelf(orc.insWeight);
            }
        }
    };
}
