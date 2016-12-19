package org.grouplens.samantha.modeler.ranking;

import com.google.common.collect.Ordering;
import org.grouplens.samantha.modeler.solver.StochasticOracle;
import org.grouplens.samantha.modeler.tree.SortingUtilities;

public class RankingUtilities {
    private RankingUtilities() {}

    static public Ordering<StochasticOracle> stochasticOracleOrdering() {
        return new Ordering<StochasticOracle>() {
            @Override
            public int compare(StochasticOracle left, StochasticOracle right) {
                return SortingUtilities.compareValues(left.getModelOutput(), right.getModelOutput());
            }
        };
    }

    static public Ordering<StochasticOracle> stochasticOracleLabelOrdering() {
        return new Ordering<StochasticOracle>() {
            @Override
            public int compare(StochasticOracle left, StochasticOracle right) {
                return SortingUtilities.compareValues(left.getLabel(), right.getLabel());
            }
        };
    }
}
