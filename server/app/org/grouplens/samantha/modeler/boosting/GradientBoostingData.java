package org.grouplens.samantha.modeler.boosting;

import it.unimi.dsi.fastutil.doubles.DoubleList;

import it.unimi.dsi.fastutil.ints.IntList;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.modeler.solver.StochasticOracle;
import org.grouplens.samantha.server.exception.BadRequestException;

import java.util.ArrayList;
import java.util.List;

public class GradientBoostingData implements LearningData {
    final private LearningData learningData;
    final private DoubleList preds;
    final private IntList subset;
    final private ObjectiveFunction objectiveFunction;
    private int idx = 0;

    public GradientBoostingData(LearningData learningData, DoubleList preds,
                                IntList subset, ObjectiveFunction objectiveFunction) {
        this.learningData = learningData;
        this.preds = preds;
        this.objectiveFunction = objectiveFunction;
        this.subset = subset;
    }

    public List<LearningInstance> getLearningInstance() {
        List<LearningInstance> instances = learningData.getLearningInstance();
        if (instances.size() == 0) {
            return instances;
        }
        List<StochasticOracle> oracles = new ArrayList<>(instances.size());
        for (LearningInstance ins : instances) {
            double modelOutput = 0.0;
            if (preds != null) {
                int subidx = idx;
                if (subset != null) {
                    subidx = subset.getInt(idx);
                }
                modelOutput += preds.getDouble(subidx);
                idx++;
            }
            StochasticOracle oracle = new StochasticOracle(modelOutput, ins.getLabel(), ins.getWeight());
            oracles.add(oracle);
        }
        oracles = objectiveFunction.wrapOracle(oracles);
        if (oracles.size() != oracles.size()) {
            throw new BadRequestException("Objective function should not change the length of the wrapped oracles. " +
                    "Use a compatible loss for gradient boosting.");
        }
        List<LearningInstance> curList = new ArrayList<>(instances.size());
        for (int i=0; i<instances.size(); i++) {
            curList.add(instances.get(i).newInstanceWithLabel(-oracles.get(i).getGradient()));
        }
        return curList;
    }

    public void startNewIteration() {
        learningData.startNewIteration();
        idx = 0;
    }
}
