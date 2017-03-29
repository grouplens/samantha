/*
 * Copyright (c) [2016-2017] [University of Minnesota]
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

package org.grouplens.samantha.server.solver;

import org.grouplens.samantha.modeler.common.LearningMethod;
import org.grouplens.samantha.modeler.solver.AsyncParallelSGD;
import org.grouplens.samantha.modeler.solver.OnlineOptimizationMethod;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class AsyncParallelSGDConfig implements LearningMethodConfig {
    private AsyncParallelSGDConfig() {}

    public static LearningMethod getLearningMethod(Configuration methodConfig,
                                                   Injector injector,
                                                   RequestContext requestContext) {
        double tol = 5.0;
        if (methodConfig.asMap().containsKey("tol")) {
            tol = methodConfig.getDouble("tol");
        }
        int maxIter = 50;
        if (methodConfig.asMap().containsKey("maxIter")) {
            maxIter = methodConfig.getInt("maxIter");
        }
        int minIter = 2;
        if (methodConfig.asMap().containsKey("minIter")) {
            minIter = methodConfig.getInt("minIter");
        }
        int num = Runtime.getRuntime().availableProcessors();
        if (methodConfig.asMap().containsKey("numProcessors")) {
            num = methodConfig.getInt("numProcessors");
        }
        OnlineOptimizationMethod onlineMethod = new AsyncParallelSGD(
                maxIter, minIter, methodConfig.getDouble("l2coef"),
                methodConfig.getDouble("learningRate"), tol, num
        );
        return onlineMethod;
    }
}
