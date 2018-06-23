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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.instance.AbstractLearningInstance;

import java.util.HashMap;
import java.util.Map;

public class TensorFlowInstance extends AbstractLearningInstance {
    @JsonProperty
    private static final long serialVersionUID = 1L;

    @JsonProperty
    protected double weight;
    @JsonProperty
    protected double label;

    @JsonProperty
    protected final Map<String, double[]> name2values;
    @JsonProperty
    protected final Map<String, int[]> name2indices;

    public TensorFlowInstance(String group) {
        super(group);
        name2values = new HashMap<>();
        name2indices = new HashMap<>();
        weight = 1.0;
        label = 0.0;
    }

    protected TensorFlowInstance(String group, double label, double weight,
                                 Map<String, double[]> name2values,
                                 Map<String, int[]> name2indices) {
        super(group);
        this.name2values = name2values;
        this.name2indices= name2indices;
        this.label = label;
        this.weight = weight;
    }

    public void putValues(String name, double[] values) {
        name2values.put(name, values);
    }

    public void putIndices(String name, int[] values) {
        name2indices.put(name, values);
    }

    public Map<String, double[]> getName2Values() {
        return name2values;
    }

    public Map<String, int[]> getName2Indices() {
        return name2indices;
    }

    public LearningInstance newInstanceWithLabel(double label) {
        TensorFlowInstance instance = new TensorFlowInstance(group, label, weight, name2values, name2indices);
        instance.label = label;
        return instance;
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
