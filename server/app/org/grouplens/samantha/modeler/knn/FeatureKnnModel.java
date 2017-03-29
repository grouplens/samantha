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

package org.grouplens.samantha.modeler.knn;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.IndexedVectorModel;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureKey;
import org.grouplens.samantha.modeler.tree.SortingUtilities;
import play.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FeatureKnnModel extends IndexedVectorModel {
    private static final long serialVersionUID = 1L;
    final private List<String> feaAttrs;
    transient final private SVDFeature svdFeature;
    final private int numNeighbors;
    final private String modelName;
    final private boolean reverse;
    final private int minSupport;

    public FeatureKnnModel(String modelName, List<String> feaAttrs,
                           int numNeighbors, boolean reverse,
                           int minSupport, SVDFeature svdFeature,
                           IndexSpace indexSpace, VariableSpace variableSpace) {
        super(modelName, 0, 2 * numNeighbors, indexSpace, variableSpace);
        this.feaAttrs = feaAttrs;
        this.svdFeature = svdFeature;
        this.numNeighbors = numNeighbors;
        this.modelName = modelName;
        this.reverse = reverse;
        this.minSupport = minSupport;
    }

    private List<double[]> getNeighbors(int curIndex, IntList svdIndices, SVDFeature svdFeature) {
        List<double[]> raw = new ArrayList<>(svdIndices.size());
        for (int target : svdIndices) {
            if (target != curIndex) {
                double[] pair = new double[2];
                pair[0] = target;
                pair[1] = svdFeature.getVectorVarByNameIndex(SVDFeatureKey.FACTORS.get(), target)
                        .cosine(svdFeature.getVectorVarByNameIndex(SVDFeatureKey.FACTORS.get(), curIndex));
                raw.add(pair);
            }
        }
        Ordering<double[]> pairDoubleOrdering = SortingUtilities.pairDoubleSecondOrdering();
        List<double[]> neighbors;
        if (reverse) {
            neighbors = pairDoubleOrdering.leastOf(raw, numNeighbors);
        } else {
            neighbors = pairDoubleOrdering.greatestOf(raw, numNeighbors);
        }
        return neighbors;
    }

    public FeatureKnnModel buildModel() {
        List<String> features = Lists.newArrayList(svdFeature.getFactorFeatures(minSupport).keySet());
        IntList svdIndices = new IntArrayList();
        IntList simIndices = new IntArrayList(features.size());
        for (int i=0; i<features.size(); i++) {
            String feature = features.get(i);
            Map<String, String> attrVals = FeatureExtractorUtilities.decomposeKey(feature);
            boolean ifModel = true;
            if (attrVals.size() == feaAttrs.size()) {
                for (String attr : feaAttrs) {
                    if (!attrVals.containsKey(attr)) {
                        ifModel = false;
                    }
                }
            } else {
                ifModel = false;
            }
            if (ifModel) {
                svdIndices.add(i);
                ensureKey(feature);
                simIndices.add(getIndexByKey(feature));
            } else {
                simIndices.add(-1);
            }
        }
        Logger.info("Total number of items to compute similarity model {}: {}",
                modelName, svdIndices.size());
        svdIndices.parallelStream().forEach(curIdx -> {
            int simIdx = simIndices.getInt(curIdx);
            List<double[]> neighbors = getNeighbors(curIdx, svdIndices, svdFeature);
            RealVector sims = getIndexVector(simIdx);
            for (int j=0; j<neighbors.size(); j++) {
                double[] neighbor = neighbors.get(j);
                sims.setEntry(j*2, simIndices.getInt((int)neighbor[0]));
                sims.setEntry(j*2+1, neighbor[1]);
            }
            setIndexVector(simIdx, sims);
        });
        return this;
    }
}
