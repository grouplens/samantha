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

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.modeler.model.IndexSpace;
import org.grouplens.samantha.modeler.model.IndexedVectorModel;
import org.grouplens.samantha.modeler.model.VariableSpace;
import org.grouplens.samantha.modeler.tree.SortingUtilities;

import java.util.*;

public class TFIDFKnnModel extends IndexedVectorModel {
    private static final long serialVersionUID = 1L;
    final private List<String> itemAttrs;
    final private List<String> feaAttrs;
    final private int numNeighbors;

    public TFIDFKnnModel(String modelName, List<String> itemAttrs,
                         List<String> feaAttrs, int numNeighbors,
                         IndexSpace indexSpace, VariableSpace variableSpace) {
        super(modelName, 0, 2 * numNeighbors, indexSpace, variableSpace);
        this.feaAttrs = feaAttrs;
        this.itemAttrs = itemAttrs;
        this.numNeighbors = numNeighbors;
    }

    public TFIDFKnnModel buildModel(EntityDAO entityDAO) {
        Map<String, Map<String, Double>> tfidf = new HashMap<>();
        Map<String, Set<String>> feaItems = new HashMap<>();
        while (entityDAO.hasNextEntity()) {
            JsonNode entity = entityDAO.getNextEntity();
            String item = FeatureExtractorUtilities.composeConcatenatedKey(entity, itemAttrs);
            String feature = FeatureExtractorUtilities.composeConcatenatedKey(entity, feaAttrs);
            Map<String, Double> feas = tfidf.getOrDefault(item, new HashMap<>());
            double freq = feas.getOrDefault(feature, 0.0);
            feas.put(feature, freq + 1.0);
            tfidf.putIfAbsent(item, feas);
            Set<String> items = feaItems.getOrDefault(feature, new HashSet<>());
            items.add(item);
            feaItems.putIfAbsent(feature, items);
        }
        double numItems = tfidf.size();
        for (Map.Entry<String, Map<String, Double>> entry : tfidf.entrySet()) {
            ensureKey(entry.getKey());
            Map<String, Double> feas = entry.getValue();
            for (String fea : feas.keySet()) {
                ensureKey(fea);
                double value = feas.get(fea) * Math.log(numItems / feaItems.get(fea).size());
                feas.put(fea, value);
            }
        }
        tfidf.entrySet().parallelStream().forEach(entry -> {
            String item = entry.getKey();
            Map<String, Double> feaVals = entry.getValue();
            List<Map.Entry<String, Double>> sortedFeaVals = new ArrayList<>(
                    feaVals.entrySet());
            sortedFeaVals.sort(SortingUtilities.mapEntryValueComparator().reversed());
            RealVector features = getKeyVector(item);
            for (int j=0; j<numNeighbors; j++) {
                if (j < sortedFeaVals.size()) {
                    features.setEntry(j*2, getIndexByKey(sortedFeaVals.get(j).getKey()));
                    features.setEntry(j*2+1, sortedFeaVals.get(j).getValue());
                } else {
                    break;
                }
            }
            setKeyVector(item, features);
        });
        return this;
    }
}
