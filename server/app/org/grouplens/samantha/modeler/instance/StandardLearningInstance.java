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

package org.grouplens.samantha.modeler.instance;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import org.grouplens.samantha.modeler.common.LearningInstance;

public class StandardLearningInstance extends AbstractLearningInstance {
    private static final long serialVersionUID = 1L;
    public static double defaultWeight = 1.0;
    public static double defaultLabel = 0.0;
    double weight;
    double label;
    final Int2DoubleMap features;

    public StandardLearningInstance(Int2DoubleMap features, double label, double weight, String group) {
        super(group);
        this.features = features;
        this.weight = weight;
        this.label = label;
    }

    public LearningInstance newInstanceWithLabel(double label) {
        return new StandardLearningInstance(this.features, label, this.weight, this.group);
    }

    public Int2DoubleMap getFeatures() {
        return features;
    }

    public double getLabel() {
        return this.label;
    }

    public double getWeight() {
        return this.weight;
    }

    public void setLabel(double label) {
        this.label = label;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
