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

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.model.IndexSpace;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SVDFeatureFactorExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    final private SVDFeature model;
    final private Map<String, List<String>> fea2svdfeas;
    final private Boolean sparse;
    final private String indexName;

    public SVDFeatureFactorExtractor(SVDFeature model, Map<String, List<String>> fea2svdfeas,
                                     Boolean sparse, String indexName) {
        this.fea2svdfeas = fea2svdfeas;
        this.model = model;
        this.sparse = sparse;
        this.indexName = indexName;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        int dim = model.getVectorVarDimensionByName(SVDFeatureKey.FACTORS.get());
        Map<String, List<Feature>> svdFeaMap = model.getFeatureMap(entity, false);
        Map<String, List<Feature>> feaMap = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : fea2svdfeas.entrySet()) {
            RealVector vector = MatrixUtils.createRealVector(new double[dim]);
            List<String> svdfeas = entry.getValue();
            boolean hit = false;
            for (String svdfea : svdfeas) {
                if (svdFeaMap.containsKey(svdfea)) {
                    List<Feature> features = svdFeaMap.get(svdfea);
                    for (Feature feature : features) {
                        vector.combineToSelf(1.0, feature.getValue(),
                                model.getVectorVarByNameIndex(SVDFeatureKey.FACTORS.get(),
                                        feature.getIndex()));
                    }
                    hit = true;
                }
            }
            if (hit == false && sparse) {
                continue;
            }
            String feaName = entry.getKey();
            List<Feature> features = new ArrayList<>();
            for (int i=0; i<dim; i++) {
                String key = FeatureExtractorUtilities.composeKey(feaName, Integer.valueOf(i).toString());
                FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(features, update,
                        indexSpace, indexName, key, vector.getEntry(i));
            }
            feaMap.put(feaName, features);
        }
        return feaMap;
    }
}
