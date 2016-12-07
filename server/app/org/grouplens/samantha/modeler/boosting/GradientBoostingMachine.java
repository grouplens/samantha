package org.grouplens.samantha.modeler.boosting;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.common.LearningMethod;
import org.grouplens.samantha.modeler.common.PredictiveModel;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.modeler.solver.StochasticOracle;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class GradientBoostingMachine {
    private final ObjectiveFunction objectiveFunction;

    @Inject
    public GradientBoostingMachine(ObjectiveFunction objectiveFunction) {
        this.objectiveFunction = objectiveFunction;
    }

    public void boostModel(DoubleList learnPreds,
                           DoubleList validPreds,
                           IntList learnSub,
                           IntList validSub,
                           PredictiveModel boostModel,
                           LearningMethod method,
                           LearningData learningData,
                           LearningData validationData) {
        LearningData learnData = new GradientBoostingData(learningData, learnPreds,
                learnSub, objectiveFunction);
        LearningData validData = null;
        if (validationData != null) {
            validData = new GradientBoostingData(validationData, validPreds, validSub, objectiveFunction);
        }
        method.learn(boostModel, learnData, validData);
    }

    public double evaluate(DoubleList preds, PredictiveModel boostModel,
                           LearningData validData, IntList subset) {
        double objVal = 0.0;
        int idx = 0;
        validData.startNewIteration();
        List<LearningInstance> instances;
        while ((instances = validData.getLearningInstance()).size() > 0) {
            List<StochasticOracle> oracles = new ArrayList<>(instances.size());
            for (LearningInstance ins : instances) {
                double modelOutput = 0.0;
                if (preds != null) {
                    int subidx = idx;
                    if (subset != null) {
                        subidx = subset.getInt(idx);
                    }
                    modelOutput += (preds.getDouble(subidx));
                    idx++;
                }
                modelOutput += boostModel.predict(ins);
                oracles.add(new StochasticOracle(modelOutput, ins.getLabel(), ins.getWeight()));
            }
            oracles = objectiveFunction.wrapOracle(oracles);
            for (StochasticOracle oracle : oracles) {
                objVal += oracle.getObjectiveValue();
            }
        }
        return objVal;
    }

    public DoubleList boostPrediction(DoubleList preds, PredictiveModel boostModel,
                                      LearningData data, IntList subset) {
        DoubleList results = preds;
        if (results == null) {
            results = new DoubleArrayList();
        }
        int idx = 0;
        data.startNewIteration();
        List<LearningInstance> instances;
        while ((instances = data.getLearningInstance()).size() > 0) {
            for (LearningInstance ins : instances) {
                double modelOutput = boostModel.predict(ins);
                if (preds == null) {
                    results.add(modelOutput);
                } else {
                    int subidx = idx;
                    if (subset != null) {
                        subidx = subset.getInt(idx);
                    }
                    modelOutput += preds.getDouble(subidx);
                    results.set(subidx, modelOutput);
                    idx++;
                }
            }
        }
        return results;
    }
}
