/*
 * Copyright (c) [2016-2018] [University of Minnesota]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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

    public List<String> getScalarNames() { return scalarNames; }
    public IntList getScalarIndexes() { return scalarIndexes; }
    public DoubleList getScalarGrads() { return scalarGrads; }
    public List<String> getVectorNames() { return vectorNames; }
    public IntList getVectorIndexes() { return vectorIndexes; }
    public List<RealVector> getVectorGrads() { return vectorGrads; }


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
