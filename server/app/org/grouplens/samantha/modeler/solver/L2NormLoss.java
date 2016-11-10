package org.grouplens.samantha.modeler.solver;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class L2NormLoss implements ObjectiveFunction {
    private static final long serialVersionUID = 1L;

    public L2NormLoss() { }

    public double wrapOutput(double output) {
        return output;
    }

    public double getObjectiveValue(double wrappedOutput, double label, double weight) {
        double err = label - wrappedOutput;
        return weight * err * err;
    }

    public double getGradient(double wrappedOutput, double label, double weight) {
        return weight * (wrappedOutput - label);
    }

    public void wrapOracle(StochasticOracle orc) {
        double err = orc.modelOutput - orc.insLabel;
        orc.objVal = err * err * orc.insWeight;
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
