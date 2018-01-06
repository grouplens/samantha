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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.solver.RandomInitializer;
import org.grouplens.samantha.server.exception.BadRequestException;

import javax.inject.Inject;

public final class SynchronizedVariableSpace implements VariableSpace {
    private static final long serialVersionUID = 1L;
    private final Map<String, DoubleList> scalarVars = new HashMap<>();
    private final Map<String, List<RealVector>> vectorVars = new HashMap<>();
    private final Lock readLock;
    private final Lock writeLock;
    private final List<Lock> readLocks = new ArrayList<>();
    private final List<Lock> writeLocks = new ArrayList<>();

    @Inject
    public SynchronizedVariableSpace() {
        ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
        readLock = rwl.readLock();
        writeLock = rwl.writeLock();
    }

    public void setSpaceState(String spaceName, SpaceMode spaceMode) {}

    public void publishSpaceVersion() {}

    private void setDoubleList(DoubleList doubleList, double value) {
        int size = doubleList.size();
        for (int i=0; i<size; i++) {
            doubleList.set(i, value);
        }
    }

    private void setDoubleList(DoubleList doubleList, RealVector vars) {
        int size = doubleList.size();
        for (int i=0; i<size; i++) {
            doubleList.set(i, vars.getEntry(i));
        }
    }

    private void setDoubleList(double[] newVar, DoubleList var) {
        int size = var.size();
        for (int i=0; i<size; i++) {
            newVar[i] = var.getDouble(i);
        }
    }

    private void initializeDoubleList(DoubleList var, double initial, boolean randomize) {
        if (randomize) {
            RandomInitializer randInit = new RandomInitializer();
            randInit.randInitDoubleList(var, false);
        } else {
            if (initial != 0.0) {
                setDoubleList(var, initial);
            }
        }
    }

    final public void requestScalarVar(String name, int size, double initial, boolean randomize) {
        DoubleList var = new DoubleArrayList(size);
        for (int i=0; i<size; i++) {
            var.add(0.0);
        }
        initializeDoubleList(var, initial, randomize);
        writeLock.lock();
        try {
            scalarVars.put(name, var);
        } finally {
            writeLock.unlock();
        }
    }

    final public boolean hasScalarVar(String name) {
        readLock.lock();
        try {
            return scalarVars.containsKey(name);
        } finally {
            readLock.unlock();
        }
    }

    final public void ensureScalarVar(String name, int size, double initial, boolean randomize) {
        writeLock.lock();
        try {
            int curSize = scalarVars.get(name).size();
            if (curSize < size) {
                DoubleList toAdd = new DoubleArrayList(size - curSize);
                for (int i=0; i<size - curSize; i++) {
                    toAdd.add(0.0);
                }
                initializeDoubleList(toAdd, initial, randomize);
                scalarVars.get(name).addAll(toAdd);
            }
            curSize = readLocks.size();
            SpaceUtilities.fillReadWriteLocks(readLocks, writeLocks, curSize, size);
        } finally {
            writeLock.unlock();
        }
    }

    final public void requestVectorVar(String name, int size, int dim, double initial,
                                       boolean randomize, boolean normalize) {
        List<RealVector> var = new ArrayList<>(size);
        for (int i=0; i<size; i++) {
            RealVector vec = MatrixUtils.createRealVector(new double[dim]);
            initializeVector(vec, initial, randomize, normalize);
            var.add(vec);
        }
        writeLock.lock();
        try {
            vectorVars.put(name, var);
        } finally {
            writeLock.unlock();
        }
    }

    final public boolean hasVectorVar(String name) {
        readLock.lock();
        try {
            return vectorVars.containsKey(name);
        } finally {
            readLock.unlock();
        }
    }

    final public void ensureVectorVar(String name, int size, int dim, double initial,
            boolean randomize, boolean normalize) {
        writeLock.lock();
        try {
            int curSize = vectorVars.get(name).size();
            if (curSize < size) {
                for (int i=curSize; i<size; i++) {
                    RealVector vec = MatrixUtils.createRealVector(new double[dim]);
                    initializeVector(vec, initial, randomize, normalize);
                    vectorVars.get(name).add(vec);
                }
            }
            curSize = readLocks.size();
            SpaceUtilities.fillReadWriteLocks(readLocks, writeLocks, curSize, size);
        } finally {
            writeLock.unlock();
        }
    }

    final public RealVector getScalarVarByName(String name) {
        readLock.lock();
        try {
            DoubleList var = scalarVars.get(name);
            double[] newVar = new double[var.size()];
            setDoubleList(newVar, var);
            return MatrixUtils.createRealVector(newVar);
        } finally {
            readLock.unlock();
        }
    }

    final public int getScalarVarSizeByName(String name) {
        readLock.lock();
        try {
            return scalarVars.get(name).size();
        } finally {
            readLock.unlock();
        }
    }

    final public void setScalarVarByName(String name, RealVector vars) {
        writeLock.lock();
        try {
            setDoubleList(scalarVars.get(name), vars);
        } finally {
            writeLock.unlock();
        }
    }

    final public double getScalarVarByNameIndex(String name, int index) {
        readLocks.get(index).lock();
        try {
            return scalarVars.get(name).getDouble(index);
        } finally {
            readLocks.get(index).unlock();
        }
    }

    final public void setScalarVarByNameIndex(String name, int index, double var) {
        writeLocks.get(index).lock();
        try {
            scalarVars.get(name).set(index, var);
        } finally {
            writeLocks.get(index).unlock();
        }
    }

    final public List<RealVector> getVectorVarByName(String name) {
        List<RealVector> vars = new ArrayList<>();
        readLock.lock();
        try {
            for (RealVector var : vectorVars.get(name)) {
                vars.add(var.copy());
            }
            return vars;
        } finally {
            readLock.unlock();
        }
    }

    final public RealMatrix getMatrixVarByName(String name) {
        readLock.lock();
        try {
            int row = getVectorVarSizeByName(name);
            int column = getVectorVarDimensionByName(name);
            RealMatrix matrix = MatrixUtils.createRealMatrix(row, column);
            for (int i=0; i<row; i++) {
                matrix.setRowVector(i, vectorVars.get(name).get(i));
            }
            return matrix;
        } finally {
            readLock.unlock();
        }
    }

    final public int getVectorVarSizeByName(String name) {
        readLock.lock();
        try {
            return vectorVars.get(name).size();
        } finally {
            readLock.unlock();
        }
    }

    final public int getVectorVarDimensionByName(String name) {
        readLock.lock();
        try {
            int varSize = vectorVars.get(name).size();
            if (varSize == 0) {
                return 0;
            } else {
                return vectorVars.get(name).get(0).getDimension();
            }
        } finally {
            readLock.unlock();
        }
    }

    final public RealVector getVectorVarByNameIndex(String name, int index) {
        readLocks.get(index).lock();
        try {
            return vectorVars.get(name).get(index).copy();
        } finally {
            readLocks.get(index).unlock();
        }
    }

    final public void setVectorVarByNameIndex(String name, int index, RealVector var) {
        writeLocks.get(index).lock();
        try {
            vectorVars.get(name).get(index).setSubVector(0, var);
        } finally {
            writeLocks.get(index).unlock();
        }
    }

    public List<String> getAllScalarVarNames() {
        readLock.lock();
        try {
            return new ArrayList<>(scalarVars.keySet());
        } finally {
            readLock.unlock();
        }
    }

    public List<String> getAllVectorVarNames() {
        readLock.lock();
        try {
            return new ArrayList<>(vectorVars.keySet());
        } finally {
            readLock.unlock();
        }
    }

    public void freeSpace() {}

    public void freeScalarVar(String name) {
        writeLock.lock();
        try {
            scalarVars.remove(name);
        } finally {
            writeLock.unlock();
        }
    }

    public void freeVectorVar(String name) {
        writeLock.lock();
        try {
            vectorVars.remove(name);
        } finally {
            writeLock.unlock();
        }
    }

    private void writeObject(ObjectOutputStream stream) {
        readLock.lock();
        try {
            stream.defaultWriteObject();
        } catch (IOException e) {
            throw new BadRequestException(e);
        } finally {
            readLock.unlock();
        }
    }
}
