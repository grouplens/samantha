package org.grouplens.samantha.modeler.solver;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.List;

public class StochasticOracle { 
    List<String> scalarNames;
    IntList scalarIndexes;
    DoubleList scalarGrads;
    List<String> vectorNames;
    IntList vectorIndexes;
    List<RealVector> vectorGrads;

    double objVal = 0.0;
    double modelOutput = 0.0;
    double gradient = 0.0;
    double insLabel = 0.0;
    double insWeight = 0.0;

    public StochasticOracle() {
        scalarNames = new ArrayList<>();
        scalarIndexes = new IntArrayList();
        scalarGrads = new DoubleArrayList();
        vectorNames = new ArrayList<>();
        vectorIndexes = new IntArrayList();
        vectorGrads = new ArrayList<>();
    }

    public void addScalarOracle(String name, int index, double grad) {
        scalarIndexes.add(index);
        scalarNames.add(name);
        scalarGrads.add(grad);
    }

    public void addVectorOracle(String name, int index, RealVector grad) {
        vectorIndexes.add(index);
        vectorNames.add(name);
        vectorGrads.add(grad);
    }

    public void setModelOutput(double modelOutput) {
        this.modelOutput = modelOutput;
    }

    public double getGradient() {
        return this.gradient;
    }

    public double getObjectiveValue() {
        return objVal;
    }

    public void setInsLabel(double insLabel) {
        this.insLabel = insLabel;
    }

    public void setInsWeight(double insWeight) {
        this.insWeight = insWeight;
    }
}
