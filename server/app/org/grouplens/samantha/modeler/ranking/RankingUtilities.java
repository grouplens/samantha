package org.grouplens.samantha.modeler.ranking;

import com.google.common.collect.Ordering;
import org.grouplens.samantha.modeler.solver.StochasticOracle;
import org.grouplens.samantha.modeler.tree.SortingUtilities;

import java.util.Comparator;

public class RankingUtilities {
    private RankingUtilities() {}

    static public Comparator<StochasticOracle> stochasticOracleReverseComparator() {
        return new Comparator<StochasticOracle>() {
            @Override
            public int compare(StochasticOracle o1, StochasticOracle o2) {
                return SortingUtilities.compareValues(o2.getModelOutput(), o1.getModelOutput());
            }
        };
    }

    static public Ordering<StochasticOracle> stochasticOracleOrdering() {
        return new Ordering<StochasticOracle>() {
            @Override
            public int compare(StochasticOracle left, StochasticOracle right) {
                return SortingUtilities.compareValues(left.getModelOutput(), right.getModelOutput());
            }
        };
    }
}
