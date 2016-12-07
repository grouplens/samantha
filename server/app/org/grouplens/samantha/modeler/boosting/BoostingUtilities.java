package org.grouplens.samantha.modeler.boosting;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.solver.StochasticOracle;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureInstance;

import java.util.List;

public class BoostingUtilities {
    private BoostingUtilities() {}

    public static void setStochasticOracles(List<LearningInstance> instances, List<StochasticOracle> oracles,
                                            SVDFeature svdfeaModel, DoubleList preds) {
        for (LearningInstance ins : instances) {
            GBCentLearningInstance centIns = (GBCentLearningInstance) ins;
            SVDFeatureInstance svdfeaIns = centIns.getSvdfeaIns();
            double pred = svdfeaModel.predict(svdfeaIns);
            preds.add(pred);
            StochasticOracle oracle = new StochasticOracle(pred, svdfeaIns.getLabel(),
                    svdfeaIns.getWeight());
            oracles.add(oracle);
        }
    }
}
