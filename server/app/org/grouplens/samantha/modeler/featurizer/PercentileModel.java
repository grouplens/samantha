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

package org.grouplens.samantha.modeler.featurizer;

import it.unimi.dsi.fastutil.doubles.Double2IntAVLTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2IntSortedMap;
import it.unimi.dsi.fastutil.doubles.Double2IntMap;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.IndexedVectorModel;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PercentileModel extends IndexedVectorModel {
    final private static Logger logger = LoggerFactory.getLogger(PercentileModel.class);
    private static final long serialVersionUID = 1L;
    final private List<TreeMap<Double, Double>> treeMaps = new ArrayList<>();
    final private double sampleRate;

    public PercentileModel(String modelName, int maxNumValues, double sampleRate,
                           IndexSpace indexSpace, VariableSpace variableSpace) {
        super(modelName, 0, maxNumValues * 2 + 1, indexSpace, variableSpace);
        this.sampleRate = sampleRate;
    }

    public double getPercentileByBuildingTreeMap(int idx, double value) {
        while (treeMaps.size() <= idx) {
            treeMaps.add(null);
        }
        TreeMap<Double, Double> treeMap = new TreeMap<>();
        RealVector values = getIndexVector(idx);
        int numValues = (int) values.getEntry(0);
        for (int i=1; i<numValues * 2; i+=2) {
            treeMap.put(values.getEntry(i), values.getEntry(i+1));
        }
        treeMaps.set(idx, treeMap);
        return getPercentileFromTreeMap(treeMap, value);
    }

    private double getPercentileFromTreeMap(TreeMap<Double, Double> treeMap, double value) {
        Map.Entry<Double, Double> entry = treeMap.ceilingEntry(value);
        if (entry == null) {
            return 1.0;
        } else {
            return entry.getValue();
        }
    }

    public double getPercentile(String attrName, double value) {
        if (hasKey(attrName)) {
            int idx = getIndexByKey(attrName);
            if (idx < treeMaps.size() && treeMaps.get(idx) != null) {
                TreeMap<Double, Double> treeMap = treeMaps.get(idx);
                return getPercentileFromTreeMap(treeMap, value);
            } else {
                return getPercentileByBuildingTreeMap(idx, value);
            }
        } else {
            return 0.5;
        }
    }

    public PercentileModel buildModel(String attrName, int numValues, EntityDAO entityDAO) {
        RealVector values = MatrixUtils.createRealVector(new double[dim]);
        Double2IntSortedMap sortedMap = new Double2IntAVLTreeMap();
        int total = 0;
        Random random = new Random();
        while (entityDAO.hasNextEntity()) {
            if (random.nextDouble() > sampleRate) {
                entityDAO.getNextEntity();
                continue;
            }
            double value = entityDAO.getNextEntity().get(attrName).asDouble();
            if (sortedMap.containsKey(value)) {
                sortedMap.put(value, sortedMap.get(value) + 1);
            } else {
                sortedMap.put(value, 1);
            }
            total++;
        }
        values.setEntry(0, numValues);
        int idx = 0;
        int cnt = 0;
        for (Double2IntMap.Entry entry : sortedMap.double2IntEntrySet()) {
            double key = entry.getDoubleKey();
            int num = entry.getIntValue();
            cnt += num;
            double value = cnt * 1.0 / total;
            double point = idx * 1.0 / numValues;
            while (point <= value) {
                values.setEntry(idx * 2 + 1, key);
                values.setEntry(idx * 2 + 2, point);
                idx++;
                point = idx * 1.0 / numValues;
            }
        }
        ensureKey(attrName);
        setKeyVector(attrName, values);
        return this;
    }
}
