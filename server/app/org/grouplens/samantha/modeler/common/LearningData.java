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

package org.grouplens.samantha.modeler.common;

import java.util.List;

/**
 * The interface representing a collection of {@link LearningInstance}.
 *
 * Typically, this is responsible in reading in data from
 * a {@link org.grouplens.samantha.modeler.dao.EntityDAO EntityDAO} and featurize each data point into
 * {@link LearningInstance}. So, highly likely, LearningData depends on
 * {@link org.grouplens.samantha.modeler.featurizer.Featurizer Featurizer}.
 */
public interface LearningData {
    /**
     * Get the next list of learning instances in the learning data.
     *
     * For each call of this method, it's supposed to return a list of {@link LearningInstance}s. Some implementations
     * group the learning instances together and load the group into the returned list.
     * If the underlying data reaches the end, it should return an empty list.
     *
     * @return if the returned list has length 0, it means the LearningData reaches the end.
     */
    List<LearningInstance> getLearningInstance();

    /**
     * Start a new iteration of the learning data so that {@link #getLearningInstance()} can return {@link LearningInstance}s
     * from the beginning of the data.
     */
    void startNewIteration();
}
