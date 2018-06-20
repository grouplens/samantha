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

package org.grouplens.samantha.modeler.xgboost;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import ml.dmlc.xgboost4j.LabeledPoint;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.instance.StandardLearningInstance;

public class XGBoostInstance implements LearningInstance {

    private final StandardLearningInstance instance;

    public XGBoostInstance(StandardLearningInstance instance) {
        this.instance = instance;
    }

    public LabeledPoint getLabeledPoint() {
        Int2DoubleMap features = instance.getFeatures();
        float[] values = new float[features.size()];
        double[] froms = features.values().toDoubleArray();
        for (int i=0; i<froms.length; i++) {
            values[i] = (float) froms[i];
        }
        return LabeledPoint.fromSparseVector((float) instance.getLabel(),
                features.keySet().toIntArray(), values);
    }

    public double getLabel() {
        return instance.getLabel();
    }

    public LearningInstance newInstanceWithLabel(double label) {
        return new XGBoostInstance((StandardLearningInstance)instance.newInstanceWithLabel(label));
    }

    public double getWeight() {
        return instance.getWeight();
    }

    public void setLabel(double label) {
        this.instance.setLabel(label);
    }

    public void setWeight(double weight) {
        this.instance.setWeight(weight);
    }
}
