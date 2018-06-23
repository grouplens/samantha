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

import org.grouplens.samantha.modeler.model.LearningModel;

import java.io.Serializable;

/**
 * The interface representing one featurized data point.
 *
 * Typically, LearningInstance is specific to certain {@link LearningModel LearningModel},
 * because different kinds of models require different numerical representation of the data. Usually, LearningInstance is
 * consisted of {@link org.grouplens.samantha.modeler.featurizer.Feature Feature}s. However, different LearningInstance
 * organize the lists of Features differently, e.g. {@link org.grouplens.samantha.modeler.svdfeature.SVDFeature SVDFeature}
 * has both global features, user features and item features in its
 * {@link org.grouplens.samantha.modeler.svdfeature.SVDFeatureInstance SVDFeatureInstance}.
 */
public interface LearningInstance extends Serializable {
    /**
     * Get the label of the data point, typical in a supervised machine learning setting.
     *
     * Currently, this is more naturally in a regression setting, i.e. the response is a real value, although a classification
     * label can be represented as a "double" type too. It will be extended to be more natural in a classification setting in the future.
     *
     * @return the label of the data point.
     */
    double getLabel();

    /**
     * Get the weight of the data point, typical in a weighted learning (e.g. regression) setting.
     *
     * @return the weight of the data point that learning algorithms
     * (e.g. {@link org.grouplens.samantha.modeler.solver.OptimizationMethod OptimizationMethod}) should emphasize.
     */
    double getWeight();

    /**
     * Set the label of the data point, which means LearningInstance is mutable.
     *
     * This is required by standard boosting
     * algorithms e.g {@link org.grouplens.samantha.modeler.boosting.StandardBoostingMethod StandardBoostingMethod}
     * where the label is changed to be the residual after a round of model learning.
     *
     * @param label the new label value of the data point.
     */
    void setLabel(double label);

    /**
     * Set the weight of the data point.
     *
     * @param weight the new weight value of the data point.
     */
    void setWeight(double weight);

    /**
     * Get the group info of the learning instance.
     *
     * By default, it's null. Each learning instance can belong to certain group. This is used while the learning
     * instances need to be grouped before feeding into a learning algorithm, e.g. learning to rank algorithms take
     * in a ordered list of data points based on relevance.
     *
     * @return the group info of the data point.
     */
    default String getGroup() {return null;}

    /**
     * Set the group info of the learning instance.
     *
     * @param group the new value of the group.
     */
    default void setGroup(String group) {}

    /**
     * Create a new instance with the new label value while keeping others the same, potentially referencing the same
     * feature lists in memory. Similar to {@link #setLabel(double)}, this is used typically in a boosting scenario.
     *
     * @param label the new label value.
     * @return the newly created learning instance but potentially with the same other contents as the current instance.
     */
    LearningInstance newInstanceWithLabel(double label);
}
