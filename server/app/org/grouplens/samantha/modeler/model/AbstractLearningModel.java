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

package org.grouplens.samantha.modeler.model;

import org.apache.commons.math3.linear.RealVector;

import java.util.List;

abstract public class AbstractLearningModel implements LearningModel {
    private static final long serialVersionUID = -2706402902132430123L;
    final protected VariableSpace variableSpace;
    final protected IndexSpace indexSpace;

    protected AbstractLearningModel(IndexSpace indexSpace, VariableSpace variableSpace) {
        this.variableSpace = variableSpace;
        this.indexSpace = indexSpace;
    }

    public void publishModel() {
        indexSpace.publishSpaceVersion();
        variableSpace.publishSpaceVersion();
    }

    public RealVector getScalarVarByName(String name) {
        return variableSpace.getScalarVarByName(name);
    }

    public int getScalarVarSizeByName(String name) {
        return variableSpace.getScalarVarSizeByName(name);
    }

    public void setScalarVarByName(String name, RealVector vars) {
        variableSpace.setScalarVarByName(name, vars);
    }

    public double getScalarVarByNameIndex(String name, int index) {
        return variableSpace.getScalarVarByNameIndex(name, index);
    }

    public void setScalarVarByNameIndex(String name, int index, double var) {
        variableSpace.setScalarVarByNameIndex(name, index, var);
    }

    public List<RealVector> getVectorVarByName(String name) {
        return variableSpace.getVectorVarByName(name);
    }

    public int getVectorVarSizeByName(String name) {
        return variableSpace.getVectorVarSizeByName(name);
    }

    public int getVectorVarDimensionByName(String name) {
        return variableSpace.getVectorVarDimensionByName(name);
    }

    public RealVector getVectorVarByNameIndex(String name, int index) {
        return variableSpace.getVectorVarByNameIndex(name, index);
    }

    public void setVectorVarByNameIndex(String name, int index, RealVector var) {
        variableSpace.setVectorVarByNameIndex(name, index, var);
    }

    public List<String> getAllScalarVarNames() {
        return variableSpace.getAllScalarVarNames();
    }

    public List<String> getAllVectorVarNames() {
        return variableSpace.getAllVectorVarNames();
    }

    public int getIndexForKey(String name, Object key) {
        return indexSpace.getIndexForKey(name, key);
    }

    public int getKeyMapSize(String name) {
        return indexSpace.getKeyMapSize(name);
    }
    public Object getKeyForIndex(String name, int index) {
        return indexSpace.getKeyForIndex(name, index);
    }

    public boolean containsKey(String name, Object key) {
        return indexSpace.containsKey(name, key);
    }
}
