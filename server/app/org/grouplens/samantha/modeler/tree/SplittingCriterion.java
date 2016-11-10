package org.grouplens.samantha.modeler.tree;

import it.unimi.dsi.fastutil.ints.IntList;

import java.io.Serializable;
import java.util.List;

public interface SplittingCriterion extends Serializable {
    void add(IntList idxList, List<double[]> resps);
    void add(int idx, List<double[]> resps);
    void remove(IntList idxList, List<double[]> resps);
    void remove(int idx, List<double[]> resps);
    double getValue(List<double[]> resps);
    double getSplittingGain(double beforeValue, SplittingCriterion leftSplit,
                            SplittingCriterion rightSplit, List<double[]> resps);
    SplittingCriterion create();
}
