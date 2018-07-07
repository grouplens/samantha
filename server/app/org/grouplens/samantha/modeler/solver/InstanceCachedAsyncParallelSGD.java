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

package org.grouplens.samantha.modeler.solver;

import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.instance.ObjectStreamLearningData;
import org.grouplens.samantha.modeler.model.LearningModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class InstanceCachedAsyncParallelSGD extends AbstractOptimizationMethod implements OnlineOptimizationMethod {
    final private static Logger logger = LoggerFactory.getLogger(InstanceCachedAsyncParallelSGD.class);
    final private String cachePath;
    final private String tstampStr = Long.valueOf(System.currentTimeMillis()).toString();
    final private int numThreads;
    final private double l2coef;
    final private double lr;

    public InstanceCachedAsyncParallelSGD(String cachePath) {
        super(5.0, 50, 2);
        this.cachePath = cachePath;
        this.numThreads = Runtime.getRuntime().availableProcessors();
        this.lr = 0.001;
        this.l2coef = 0.0;
    }

    public InstanceCachedAsyncParallelSGD(int maxIter, int minIter, double l2coef,
                                          double learningRate, double tol,
                                          int numThreads, String cachePath) {
        super(tol, maxIter, minIter);
        this.cachePath = cachePath;
        this.numThreads = numThreads;
        this.l2coef = l2coef;
        this.lr = learningRate;
    }

    private void cacheLearningData(LearningData data, String prefix) {
        List<Thread> threads = new ArrayList<>(numThreads);
        List<ObjectiveRunnable> runnables = new ArrayList<>(numThreads);
        for (int i=0; i<numThreads; i++) {
            String oneCachePath = getCachePath(prefix, i);
            ObjectiveRunnable runnable = new CacheInstanceRunnable(oneCachePath, data);
            runnables.add(runnable);
            Thread thread = new Thread(runnable);
            threads.add(thread);
            thread.start();
        }
        double cnt = SolverUtilities.joinObjectiveRunnableThreads(numThreads, runnables, threads);
        logger.info("Done Caching. Cached {} instances totally.", cnt);
    }

    private void clearCache(String prefix) {
        for (int i = 0; i < numThreads; i++) {
            String oneCachePath = getCachePath(prefix, i);
            new File(oneCachePath).delete();
        }
    }

    private String getCachePath(String prefix, Integer thrIdx) {
        return Paths.get(cachePath, prefix + "-" + thrIdx.toString() + "-" +
                tstampStr + ".tmp").toString();
    }

    public double minimize(LearningModel model, LearningData learningData, LearningData validData) {
        cacheLearningData(learningData, "learn");
        if (validData != null) {
            cacheLearningData(validData, "valid");
        }
        TerminationCriterion learnCrit = new TerminationCriterion(tol, maxIter, minIter);
        TerminationCriterion validCrit = null;
        if (validData != null) {
            validCrit = new TerminationCriterion(tol, maxIter, minIter);
        }
        logger.info("Using numThreads={}", numThreads);
        List<Thread> threads = new ArrayList<>(numThreads);
        List<ObjectiveRunnable> runnables = new ArrayList<>(numThreads);
        double learnObjVal = 0.0;
        while (learnCrit.keepIterate()) {
            if (validCrit != null && !(validCrit.keepIterate())) {
                break;
            }
            threads.clear();
            runnables.clear();
            for (int i=0; i<numThreads; i++) {
                String oneCachePath = getCachePath("learn", i);
                SolverUtilities.startObjectiveRunnableThreads(oneCachePath, model, l2coef, lr,
                        runnables, threads);
            }
            learnObjVal = SolverUtilities.joinObjectiveRunnableThreads(numThreads, runnables, threads);
            learnCrit.addIteration(InstanceCachedAsyncParallelSGD.class.toString()
                    + " -- Learning", learnObjVal);
            if (validData != null) {
                threads.clear();
                runnables.clear();
                for (int i=0; i<numThreads; i++) {
                    String oneCachePath = getCachePath("valid", i);
                    LearningData learnData = new ObjectStreamLearningData(oneCachePath);
                    EvaluateRunnable runnable = new EvaluateRunnable(model, learnData);
                    runnables.add(runnable);
                    Thread thread = new Thread(runnable);
                    threads.add(thread);
                    thread.start();
                }
                double validObjVal = SolverUtilities.joinObjectiveRunnableThreads(numThreads, runnables, threads);
                validCrit.addIteration(InstanceCachedAsyncParallelSGD.class.toString()
                        + " -- Validating", validObjVal);
            }
        }
        clearCache("learn");
        if (validData != null) {
            clearCache("valid");
        }
        return learnObjVal;
    }

    public double update(LearningModel learningModel, LearningData learningData) {
        cacheLearningData(learningData, "update");
        List<Thread> threads = new ArrayList<>(numThreads);
        List<ObjectiveRunnable> runnables = new ArrayList<>(numThreads);
        logger.info("Using numThreads={}", numThreads);
        for (int i=0; i<numThreads; i++) {
            String oneCachePath = getCachePath("update", i);
            SolverUtilities.startObjectiveRunnableThreads(oneCachePath, learningModel, l2coef,
                    lr, runnables, threads);
        }
        double objVal = SolverUtilities.joinObjectiveRunnableThreads(numThreads, runnables, threads);
        clearCache("update");
        return objVal;
    }
}
