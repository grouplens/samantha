package org.grouplens.samantha.modeler.solver;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.List;

public class StochasticOracle { 
    final List<String> scalarNames = new ArrayList<>();
    final IntList scalarIndexes = new IntArrayList();
    final DoubleList scalarGrads = new DoubleArrayList();
    final List<String> vectorNames = new ArrayList<>();
    final IntList vectorIndexes = new IntArrayList();
    final List<RealVector> vectorGrads = new ArrayList<>();

    private double objVal = 0.0;
    private double gradient = 0.0;
    private double label = 0.0;
    private double weight = 0.0;
    private double modelOutput = 0.0;

    public StochasticOracle() {}

    public StochasticOracle(double modelOutput, double label, double weight) {
        this.modelOutput = modelOutput;
        this.label = label;
        this.weight = weight;
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

    public double getModelOutput() {
        return this.modelOutput;
    }

    public double getGradient() {
        return this.gradient;
    }

    public double getObjectiveValue() {
        return objVal;
    }

    public double getLabel() {
        return label;
    }

    public double getWeight() {
        return weight;
    }

    public void setObjVal(double objVal) {
        this.objVal = objVal;
    }

    public void setGradient(double gradient) {
        this.gradient = gradient;
        for (int i=0; i<scalarGrads.size(); i++) {
            scalarGrads.set(i, scalarGrads.getDouble(i) * gradient);
        }
        for (int i=0; i<vectorGrads.size(); i++) {
            vectorGrads.get(i).mapMultiplyToSelf(gradient);
        }
    }

    public void setValues(double modelOutput, double insLabel, double insWeight) {
        this.modelOutput = modelOutput;
        this.label = insLabel;
        this.weight = insWeight;
    }
}
