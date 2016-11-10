package org.grouplens.samantha.modeler.featurizer;

import org.apache.commons.math3.analysis.UnivariateFunction;

import static java.lang.Math.abs;

public class SelfPlusOneRatioFunction implements UnivariateFunction {

    public double value(double val) {
        return val / (1.0 + abs(val));
    }
}
