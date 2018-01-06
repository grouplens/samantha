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

package org.grouplens.samantha.modeler.common;

import org.grouplens.samantha.modeler.model.SpaceModel;

import java.util.List;

/**
 * The interface representing a predictive model, which takes in an {@link LearningInstance} and produces a value.
 */
public interface PredictiveModel extends SpaceModel {
    /**
     * Make prediction on a {@link LearningInstance} or data point.
     *
     * @param ins the learning instance to make prediction on.
     * @return the predicted values based on the model, which supports both regression with single output or
     *     classification with multiple outputs.
     */
    double[] predict(LearningInstance ins);

    /**
     * Make prediction on a list of {@link LearningInstance}s or data points.
     *
     * @param instances the learning instances to make prediction on.
     * @return the predicted values based on the model, which supports both regression with single output or
     *     classification with multiple outputs. The first dimension represents the number of instances while
     *     the second dimension represents the number of outputs for each instance. The introduction of this
     *     method is to support matrix-like computation to enable more parallel such as TensorFlow. The default
     *     implementation is treating the problem as regression.
     */
    default double[][] predict(List<LearningInstance> instances) {
        double[][] preds = new double[instances.size()][1];
        for (int i=0; i<instances.size(); i++) {
            double[] insPreds = predict(instances.get(i));
            preds[i][0] = insPreds[0];
        }
        return preds;
    }
}
