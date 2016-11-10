package org.grouplens.samantha.modeler.svdfeature;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.IndexedVectorModel;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.grouplens.samantha.server.ranker.RankerUtilities;
import play.Logger;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FeatureKnnModel extends IndexedVectorModel {
    private static final long serialVersionUID = 1L;
    final private List<String> feaAttrs;
    transient final private SVDFeatureModel svdFeatureModel;
    final private int numNeighbors;
    final private String modelName;
    final private boolean reverse;
    final private int minSupport;

    public FeatureKnnModel(String modelName, List<String> feaAttrs,
                           int numNeighbors, boolean reverse,
                           int minSupport, SVDFeatureModel svdFeatureModel,
                           IndexSpace indexSpace, VariableSpace variableSpace) {
        super(modelName, 0, 2 * numNeighbors, indexSpace, variableSpace);
        this.feaAttrs = feaAttrs;
        this.svdFeatureModel = svdFeatureModel;
        this.numNeighbors = numNeighbors;
        this.modelName = modelName;
        this.reverse = reverse;
        this.minSupport = minSupport;
    }

    private List<double[]> getNeighbors(int curIndex, IntList svdIndices, SVDFeatureModel svdFeatureModel) {
        List<double[]> raw = new ArrayList<>(svdIndices.size());
        for (int target : svdIndices) {
            if (target != curIndex) {
                double[] pair = new double[2];
                pair[0] = target;
                pair[1] = svdFeatureModel.getVectorVarByNameIndex(SVDFeatureKey.FACTORS.get(), target)
                        .cosine(svdFeatureModel.getVectorVarByNameIndex(SVDFeatureKey.FACTORS.get(), curIndex));
                raw.add(pair);
            }
        }
        Ordering<double[]> pairDoubleOrdering = RankerUtilities.pairDoubleSecondOrdering();
        List<double[]> neighbors;
        if (reverse) {
            neighbors = pairDoubleOrdering.leastOf(raw, numNeighbors);
        } else {
            neighbors = pairDoubleOrdering.greatestOf(raw, numNeighbors);
        }
        return neighbors;
    }

    public FeatureKnnModel buildModel() {
        List<String> features = Lists.newArrayList(svdFeatureModel.getFactorFeatures(minSupport).keySet());
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
            List<double[]> neighbors = getNeighbors(curIdx, svdIndices, svdFeatureModel);
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
