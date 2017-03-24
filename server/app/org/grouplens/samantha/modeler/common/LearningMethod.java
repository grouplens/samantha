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

/**
 * The interface for representing the algorithm/method of learning a {@link PredictiveModel}.
 *
 * This can be an optimization method or greedy tree learning method etc. This method is critical for a standard
 * boosting procedure. I.e. ideally, we should be able to have any predictive models and boost them together following
 * the standard boosting procedure. See {@link org.grouplens.samantha.modeler.boosting.GradientBoostingMachine GradientBoostingMachine}
 * for an example.
 */
public interface LearningMethod {
    /**
     * Learn a predictive model based on the input learning data sets.
     *
     * @param model the predictive model to learn.
     * @param learningData the learning data to use to learn the model, i.e. training data.
     * @param validData the validation data to use while learning, e.g. for early stopping etc. This learning data
     *                  could be null in which the method doesn't take into account the validation process or doesn't
     *                  rely on a separate data set to prevent over-fitting.
     */
    void learn(PredictiveModel model, LearningData learningData, LearningData validData);
}
