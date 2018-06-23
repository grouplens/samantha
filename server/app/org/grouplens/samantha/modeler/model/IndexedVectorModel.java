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

package org.grouplens.samantha.modeler.model;

import org.apache.commons.math3.linear.RealVector;

public class IndexedVectorModel implements SpaceModel {
    private static final long serialVersionUID = 1L;
    final private String modelName;
    final protected int dim;
    final private IndexSpace indexSpace;
    final private VariableSpace variableSpace;

    public IndexedVectorModel(String modelName, int initialSize, int dim,
                              IndexSpace indexSpace, VariableSpace variableSpace) {
        this.modelName = modelName;
        this.dim = dim;
        this.indexSpace = indexSpace;
        this.variableSpace = variableSpace;
        this.indexSpace.requestKeyMap(modelName);
        this.variableSpace.requestVectorVar(modelName, initialSize, dim, 0.0, false, false);
    }

    public int ensureKey(String key) {
        int index = indexSpace.setKey(modelName, key);
        variableSpace.ensureVectorVar(modelName, index + 1, dim, 0.0, false, false);
        return index;
    }

    public int getIndexByKey(String key) {
        return indexSpace.getIndexForKey(modelName, key);
    }

    public int getIndexSize() {
        return indexSpace.getKeyMapSize(modelName);
    }

    public boolean hasKey(String key) {
        return indexSpace.containsKey(modelName, key);
    }

    public RealVector getKeyVector(String key) {
        int index = indexSpace.getIndexForKey(modelName, key);
        return variableSpace.getVectorVarByNameIndex(modelName, index);
    }

    public RealVector getIndexVector(int index) {
        return variableSpace.getVectorVarByNameIndex(modelName, index);
    }

    public String getKeyByIndex(int index) {
        return (String)indexSpace.getKeyForIndex(modelName, index);
    }

    public void setKeyVector(String key, RealVector vector) {
        int index = indexSpace.getIndexForKey(modelName, key);
        variableSpace.setVectorVarByNameIndex(modelName, index, vector);
    }

    public void setIndexVector(int index, RealVector vector) {
        variableSpace.setVectorVarByNameIndex(modelName, index, vector);
    }

    public void publishModel() {
        indexSpace.publishSpaceVersion();
        variableSpace.publishSpaceVersion();
    }
}
