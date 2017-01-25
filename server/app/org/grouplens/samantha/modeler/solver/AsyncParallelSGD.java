package org.grouplens.samantha.modeler.solver;

import org.grouplens.samantha.modeler.common.LearningData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AsyncParallelSGD extends AbstractOptimizationMethod  implements OnlineOptimizationMethod {
    final private static Logger logger = LoggerFactory.getLogger(AsyncParallelSGD.class);
    final private double l2coef;
    final private double lr;
    final private int numThreads;

    public AsyncParallelSGD() {
        super(5.0, 50, 2);
        l2coef = 0.0;
        lr = 0.001;
        numThreads = Runtime.getRuntime().availableProcessors();
    }

    public AsyncParallelSGD(int maxIter, int minIter, double l2coef,
                            double learningRate, double tol, int numThreads) {
        super(tol, maxIter, minIter);
        this.l2coef = l2coef;
        this.lr = learningRate;
        this.numThreads = numThreads;
    }

    /**
     * @param learningData must be synchronized.
     */
    public double update(LearningModel model, LearningData learningData) {
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
        double objVal = SolverUtilities.joinObjectiveRunnableThreads(numThreads, runnables, threads);
        return objVal;
    }
}
