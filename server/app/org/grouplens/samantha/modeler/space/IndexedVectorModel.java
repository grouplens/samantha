package org.grouplens.samantha.modeler.space;

import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.SpaceProducer;
import org.grouplens.samantha.modeler.space.VariableSpace;
import play.inject.Injector;

import java.io.Serializable;

public class IndexedVectorModel implements Serializable {
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

    public void ensureKey(String key) {
        if (!indexSpace.containsKey(modelName, key)) {
            indexSpace.setKey(modelName, key);
        }
        int index = indexSpace.getIndexForKey(modelName, key);
        variableSpace.ensureVectorVar(modelName, index + 1, dim, 0.0, false, false);
    }

    public int getIndexByKey(String key) {
        return indexSpace.getIndexForKey(modelName, key);
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
}
