package org.grouplens.samantha.modeler.knn;

import com.fasterxml.jackson.databind.node.ObjectNode;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.modeler.tree.SortingUtilities;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KnnModelFeatureTrigger {
    final private List<String> feaAttrs;
    final private String weightAttr;
    final private String scoreAttr;
    final private FeatureKnnModel featureKnnModel;
    final private FeatureKnnModel featureKdnModel;
    final private int maxInter = 2000;

    /**
     * @param featureKnnModel could be null.
     * @param featureKdnModel could be null.
     * @param weightAttr the value of this attribute determines which model to use.
     *                   if value >= 0.5, featureKnnModel is used.
     *                   if value < 0.5, featureKdnModel is used.
     */
    public KnnModelFeatureTrigger(FeatureKnnModel featureKnnModel, FeatureKnnModel featureKdnModel,
                                  List<String> feaAttrs, String weightAttr, String scoreAttr) {
        this.feaAttrs = feaAttrs;
        this.weightAttr = weightAttr;
        this.scoreAttr = scoreAttr;
        this.featureKnnModel = featureKnnModel;
        this.featureKdnModel = featureKdnModel;
    }

    private void getNeighbors(Object2DoubleMap<String> item2score, FeatureKnnModel knnModel,
                              String key, double weight) {
        if (knnModel.hasKey(key)) {
            RealVector sims = knnModel.getKeyVector(key);
            for (int i=0; i<sims.getDimension(); i+=2) {
                int idx = (int)sims.getEntry(i);
                double sim = sims.getEntry(i+1);
                String recItem = knnModel.getKeyByIndex(idx);
                double oldVal = item2score.getOrDefault(recItem, 0.0);
                item2score.put(recItem, weight * sim + oldVal);
            }
        }
    }

    private void getNeighbors(ObjectSet<String> items, FeatureKnnModel knnModel,
                              String key) {
        if (knnModel.hasKey(key)) {
            RealVector sims = knnModel.getKeyVector(key);
            for (int i=0; i<sims.getDimension(); i+=2) {
                int idx = (int)sims.getEntry(i);
                String recItem = knnModel.getKeyByIndex(idx);
                items.add(recItem);
            }
        }
    }

    public List<ObjectNode> getTriggeredFeatures(List<ObjectNode> bases) {
        Object2DoubleMap<String> item2score = new Object2DoubleOpenHashMap<>();
        int numInter = 0;
        for (ObjectNode inter : bases) {
            double weight = 1.0;
            if (inter.has(weightAttr)) {
                weight = inter.get(weightAttr).asDouble();
            }
            String key = FeatureExtractorUtilities.composeConcatenatedKey(inter, feaAttrs);
            if (weight >= 0.5 && featureKnnModel != null) {
                getNeighbors(item2score, featureKnnModel, key, weight);
            }
            if (weight < 0.5 && featureKdnModel != null) {
                getNeighbors(item2score, featureKdnModel, key, weight);
            }
            numInter++;
            if (numInter >= maxInter) {
                break;
            }
        }
        List<ObjectNode> results = new ArrayList<>();
        for (Map.Entry<String, Double> entry : item2score.entrySet()) {
            ObjectNode entity = Json.newObject();
            Map<String, String> attrVals = FeatureExtractorUtilities.decomposeKey(entry.getKey());
            for (Map.Entry<String, String> ent : attrVals.entrySet()) {
                entity.put(ent.getKey(), ent.getValue());
            }
            entity.put(scoreAttr, entry.getValue());
            results.add(entity);
        }
        results.sort(SortingUtilities.jsonFieldReverseComparator(scoreAttr));
        return results;
    }

    public List<ObjectNode> getTriggeredFeaturesWithoutScore(List<ObjectNode> bases) {
        ObjectSet<String> items = new ObjectOpenHashSet<>();
        for (ObjectNode inter : bases) {
            double weight = 1.0;
            if (inter.has(weightAttr)) {
                weight = inter.get(weightAttr).asDouble();
            }
            String key = FeatureExtractorUtilities.composeConcatenatedKey(inter, feaAttrs);
            if (weight >= 0.5 && featureKnnModel != null) {
                getNeighbors(items, featureKnnModel, key);
            }
            if (weight < 0.5 && featureKdnModel != null) {
                getNeighbors(items, featureKdnModel, key);
            }
        }
        List<ObjectNode> results = new ArrayList<>();
        for (String item : items) {
            ObjectNode entity = Json.newObject();
            Map<String, String> attrVals = FeatureExtractorUtilities.decomposeKey(item);
            for (Map.Entry<String, String> ent : attrVals.entrySet()) {
                entity.put(ent.getKey(), ent.getValue());
            }
            results.add(entity);
        }
        return results;
    }
}
