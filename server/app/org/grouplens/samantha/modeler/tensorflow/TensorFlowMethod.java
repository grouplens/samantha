/*
 * Copyright (c) [2016-2018] [University of Minnesota]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.grouplens.samantha.modeler.tensorflow;

import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.model.LearningModel;
import org.grouplens.samantha.modeler.solver.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TensorFlowMethod extends AbstractOptimizationMethod implements OnlineOptimizationMethod {
    private static Logger logger = LoggerFactory.getLogger(SolverUtilities.class);
    final private int numThreads;

    public TensorFlowMethod(double tol, int maxIter, int minIter, int numThreads) {
        super(tol, maxIter, minIter);
        this.numThreads = numThreads;
    }

    private class TensorFlowRunnable implements ObjectiveRunnable {
        private final LearningModel learningModel;
        private final LearningData learningData;
        private double objVal = 0.0;

        TensorFlowRunnable(LearningModel learningModel, LearningData learningData) {
            this.learningData = learningData;
            this.learningModel = learningModel;
        }

        public void run() {
            ObjectiveFunction objFunc = learningModel.getObjectiveFunction();
            List<LearningInstance> instances;
            int cnt = 0;
            while ((instances = learningData.getLearningInstance()).size() > 0) {
                List<StochasticOracle> oracles = learningModel.getStochasticOracle(instances);
                objFunc.wrapOracle(oracles);
                for (StochasticOracle oracle : oracles) {
                    logger.debug("Loss value: {}", oracle.getObjectiveValue());
                    objVal += oracle.getObjectiveValue();
                }
                cnt += instances.size();
                if (cnt % 100000 == 0) {
                    logger.info("Updated the model using {} instances.", cnt);
                }
            }
        }

        public double getObjVal() {
            return objVal;
        }
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
            TensorFlowRunnable runnable = new TensorFlowRunnable(model, learningData);
            runnables.add(runnable);
            Thread thread = new Thread(runnable);
            threads.add(thread);
            thread.start();
        }
        double objVal = SolverUtilities.joinObjectiveRunnableThreads(numThreads, runnables, threads);
        return objVal;
    }
}
