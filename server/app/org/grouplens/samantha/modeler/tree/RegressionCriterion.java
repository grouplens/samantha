package org.grouplens.samantha.modeler.tree;

import com.google.inject.ImplementedBy;

@ImplementedBy(MeanDivergence.class)
public interface RegressionCriterion extends SplittingCriterion {
}
