package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureKey;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SVDFeatureFactorExtractor implements FeatureExtractor {
    final private SVDFeatureModel model;
    final private Map<String, List<String>> fea2svdfeas;
    final private Boolean sparse;
    final private String indexName;

    public SVDFeatureFactorExtractor(SVDFeatureModel model, Map<String, List<String>> fea2svdfeas,
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
