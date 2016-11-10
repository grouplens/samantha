package org.grouplens.samantha.modeler.solver;

import org.grouplens.samantha.modeler.common.LearningData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AsyncParallelSGD extends AbstractOnlineOptimizationMethod {
    final private static Logger logger = LoggerFactory.getLogger(AsyncParallelSGD.class);
    final private double l2coef;
    final private double lr;
    final private int numThreads;

    public AsyncParallelSGD() {
        super();
        l2coef = 0.0;
        lr = 0.001;
        tol = 5.0;
        maxIter = 50;
        numThreads = Runtime.getRuntime().availableProcessors();
    }

    public AsyncParallelSGD(int maxIter, double l2coef, double learningRate, double tol,
                            int numThreads) {
        super();
        this.l2coef = l2coef;
        this.lr = learningRate;
        this.tol = tol;
        this.maxIter = maxIter;
        this.numThreads = numThreads;
    }

    /**
     * @param learningData must be synchronized.
     */
    public double update(LearningModel model, LearningData learningData) {
        double objVal = 0.0;
        L2Regularizer l2term = new L2Regularizer();
        if (l2coef != 0.0) {
            objVal += SolverUtilities.getL2RegularizationObjective(model, l2term, l2coef);
        }
        learningData.startNewIteration();
        logger.info("Using numThreads={}", numThreads);
        List<Thread> threads = new ArrayList<>(numThreads);
        List<ObjectiveRunnable> runnables = new ArrayList<>(numThreads);
        for (int i=0; i<numThreads; i++) {
            SGDRunnable sgdRunnable = new SGDRunnable(model, learningData, l2coef, lr);
            runnables.add(sgdRunnable);
            Thread thread = new Thread(sgdRunnable);
            threads.add(thread);
            thread.start();
        }
        objVal += SolverUtilities.joinObjectiveRunnableThreads(numThreads, runnables, threads);
        return objVal;
    }
}
