package org.grouplens.samantha.modeler.boosting;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningMethod;
import org.grouplens.samantha.modeler.common.PredictiveModel;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class StandardBoostingMethod implements LearningMethod {
    private static Logger logger = LoggerFactory.getLogger(StandardBoostingMethod.class);
    final private int maxIter;

    @Inject
    public StandardBoostingMethod() {
        this.maxIter = 100;
    }

    public StandardBoostingMethod(int maxIter) {
        this.maxIter = maxIter;
    }

    public double boost(BoostedPredictiveModel model, LearningData learnData, LearningData validData) {
        ObjectiveFunction objFunc = model.getObjectiveFunction();
        GradientBoostingMachine gbm = new GradientBoostingMachine(objFunc);
        DoubleList preds = null;
        DoubleList valids = null;
        double objVal = Double.MAX_VALUE;
        int bestIter = 0;
        for (int i=0; i<maxIter; i++) {
            logger.info("Iteration {} learning.", i + 1);
            PredictiveModel component = model.getPredictiveModel();
            LearningMethod method = model.getLearningMethod();
            gbm.boostModel(preds, valids, null, null, component, method, learnData, validData);
            if (validData != null) {
                double obj = gbm.evaluate(valids, component, validData, null);
                if (obj < objVal) {
                    objVal = obj;
                    bestIter = i;
                }
                logger.info("Iteration {}: {}", i + 1, objVal);
                valids = gbm.boostPrediction(valids, component, validData, null);
            }
            preds = gbm.boostPrediction(preds, component, learnData, null);
            model.addPredictiveModel(component);
        }
        if (objVal != Double.MAX_VALUE) {
            logger.info("The best iteration is {} with objVal {}.", bestIter + 1, objVal);
            model.setBestIteration(bestIter);
        }
        return objVal;
    }

    public void learn(PredictiveModel model, LearningData learningData, LearningData validData) {
        BoostedPredictiveModel boostedPredictiveModel = (BoostedPredictiveModel) model;
        boost(boostedPredictiveModel, learningData, validData);
    }
}
