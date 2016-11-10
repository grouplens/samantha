package org.grouplens.samantha.modeler.solver;

import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.server.exception.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SolverUtilities {

    private SolverUtilities() {}

    private static Logger logger = LoggerFactory.getLogger(SolverUtilities.class);

    public static double getL2RegularizationObjective(LearningModel model, L2Regularizer l2term, double l2coef) {
        double objVal = 0.0;
        List<String> allScalarVarNames = model.getAllScalarVarNames();
        for (String name : allScalarVarNames) {
            RealVector var = model.getScalarVarByName(name);
            objVal += l2term.getObjective(l2coef, var);
        }
        List<String> allVectorVarNames = model.getAllVectorVarNames();
        for (String name : allVectorVarNames) {
            List<RealVector> vars = model.getVectorVarByName(name);
            objVal += l2term.getObjective(l2coef, vars);
        }
        return objVal;
    }

    public static double stochasticGradientDescentUpdate(LearningModel model, ObjectiveFunction objFunc,
                                                         LearningData learningData, L2Regularizer l2term,
                                                         double l2coef, double lr) {
        int cnt = 0;
        double objVal = 0.0;
        LearningInstance ins;
        while ((ins = learningData.getLearningInstance()) != null) {
            StochasticOracle orc = model.getStochasticOracle(ins);
            objFunc.wrapOracle(orc);
            objVal += orc.objVal;
            if (Double.isNaN(objVal)) {
                logger.error("Objective value becomes NaN at {}th instance.", cnt);
                throw new InvalidRequestException("Got NaN error.");
            }
            for (int i = 0; i < orc.scalarNames.size(); i++) {
                String name = orc.scalarNames.get(i);
                int idx = orc.scalarIndexes.getInt(i);
                double grad = orc.scalarGrads.getDouble(i);
                double var = model.getScalarVarByNameIndex(name, idx);
                model.setScalarVarByNameIndex(name, idx, var - lr * (grad + l2coef * l2term.getGradient(var)));
            }
            for (int i = 0; i < orc.vectorNames.size(); i++) {
                String name = orc.vectorNames.get(i);
                int idx = orc.vectorIndexes.getInt(i);
                RealVector var = model.getVectorVarByNameIndex(name, idx);
                RealVector grad = orc.vectorGrads.get(i);
                model.setVectorVarByNameIndex(name, idx, var.combineToSelf(1.0, -lr,
                        l2term.addGradient(grad, var, l2coef)));
            }
            cnt++;
            if (cnt % 100000 == 0) {
                logger.info("Updated the model using {} instances.", cnt);
            }
        }
        return objVal;
    }

    public static double joinObjectiveRunnableThreads(int numThreads, List<ObjectiveRunnable> runnables, List<Thread> threads) {
        double objVal = 0.0;
        for (int i=0; i<numThreads; i++) {
            try {
                threads.get(i).join();
                objVal += runnables.get(i).getObjVal();
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }
        return objVal;
    }

    public static void startObjectiveRunnableThreads(String oneCachePath, LearningModel learningModel,
                                                     double l2coef, double lr, List<ObjectiveRunnable> runnables,
                                                     List<Thread> threads) {
        LearningData learnData = new ObjectStreamLearningData(oneCachePath);
        SGDRunnable runnable = new SGDRunnable(learningModel, learnData, l2coef, lr);
        runnables.add(runnable);
        Thread thread = new Thread(runnable);
        threads.add(thread);
        thread.start();
    }

    public static double evaluate(LearningModel model, LearningData validData) {
        double objVal = 0.0;
        ObjectiveFunction objFunc = model.getObjectiveFunction();
        LearningInstance ins;
        validData.startNewIteration();
        while ((ins = validData.getLearningInstance()) != null) {
            StochasticOracle orc = model.getStochasticOracle(ins);
            objFunc.wrapOracle(orc);
            objVal += orc.objVal;
        }
        return objVal;
    }
}
