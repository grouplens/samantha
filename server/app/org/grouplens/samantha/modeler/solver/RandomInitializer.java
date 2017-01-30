package org.grouplens.samantha.modeler.solver;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.apache.commons.math3.linear.MatrixUtils;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.Random;

public class RandomInitializer {
    final private double multi;
    final private double subtract;
    final private Random rand;

    public RandomInitializer() {
        this.multi = 0.1;
        this.subtract = 0.5;
        this.rand = new Random();
    }

    public RandomInitializer(long seed, double multiplier, double subtract) {
        this.multi = multiplier;
        this.subtract = subtract;
        this.rand = new Random(seed);
    }

    public double randInitValue() {
        return (rand.nextDouble() - subtract) * multi;
    }

    public void randInitVector(RealVector vec, boolean normalize) {
        int len = vec.getDimension();
        double sum = 0.0;
        for (int i=0; i<len; i++) {
            double val;
            if (normalize) {
                val = rand.nextDouble();
            } else {
                val = (rand.nextDouble() - subtract) * multi;
            }
            vec.setEntry(i, val);
            if (normalize) {
                sum += val;
            }
        }
        if (normalize) {
            vec.mapDivideToSelf(sum);
        }
    }

    public void randInitDoubleList(DoubleList doubleList, boolean normalize) {
        int size = doubleList.size();
        double sum = 0.0;
        for (int i=0; i<size; i++) {
            double val;
            if (normalize) {
                val = rand.nextDouble();
            } else {
                val = (rand.nextDouble() - subtract) * multi;
            }
            doubleList.set(i, val);
            if (normalize) {
                sum += val;
            }
        }
        if (normalize) {
            for (int i=0; i<size; i++) {
                doubleList.set(i, doubleList.getDouble(i) / sum);
            }
        }
    }

    public void randInitMatrix(RealMatrix mat, boolean normalize) {
        int len = mat.getRowDimension();
        RealVector vec = MatrixUtils.createRealVector(new double[mat.getColumnDimension()]);
        for (int i=0; i<len; i++) {
            randInitVector(vec, normalize);
            mat.setRowVector(i, vec);
        }
    }
}
