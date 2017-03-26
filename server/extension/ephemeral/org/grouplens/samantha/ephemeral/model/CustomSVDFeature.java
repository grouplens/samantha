package org.grouplens.samantha.ephemeral.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.featurizer.Feature;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureKey;
import org.grouplens.samantha.server.exception.ConfigurationException;
import org.grouplens.samantha.server.retriever.RetrieverUtilities;
import play.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class CustomSVDFeature extends SVDFeature {
    private List<ObjectNode> entities;

    private RealVector averageUserVector;

    public RealVector getAverageUserVector() {
        if (averageUserVector == null) {
            List<Integer> indices = new ArrayList<>();

            int size = getKeyMapSize(SVDFeatureKey.FACTORS.get());
            for (int i = 0; i < size; i++) {
                String key = (String) getKeyForIndex(SVDFeatureKey.FACTORS.get(), i);
                if (key.startsWith("userId")) {
                    indices.add(i);
                } else if (!key.startsWith("movieId")) {
                    throw new ConfigurationException("encountered vector variable that didn't start with userId or movieId");
                }
            }

            if (indices.isEmpty()) {
                throw new ConfigurationException("No userId vectors found");
            }

            averageUserVector = indices.stream()
                    .map(index -> getVectorVarByNameIndex(SVDFeatureKey.FACTORS.get(), index))
                    .reduce((v1, v2) -> v1.add(v2))
                    .get().mapDivide(indices.size());
        }
        return averageUserVector;
    }

    public void setEntities(List<ObjectNode> entities) {
        Ordering<ObjectNode> ordering = RetrieverUtilities.jsonFieldOrdering("popularity").reverse();
        this.entities = ordering.immutableSortedCopy(entities);

        // For logging correctness of results...
        List<Integer> top10 = this.entities.subList(0, 10).stream().map(obj -> obj.get("movieId").asInt()).collect(Collectors.toList());
        Logger.info("Top 10 most popular items are: {}", top10.toString());
    }
    public List<ObjectNode> getEntities() {
        return entities.stream()
                .map(x -> x.deepCopy()) //
                .collect(Collectors.toList());
    }
    public List<ObjectNode> getEntities(int limit) {
        return entities.subList(0, Math.min(limit, entities.size())).stream()
                .map(x -> x.deepCopy())
                .collect(Collectors.toList());
    }

    static public CustomSVDFeature createSVDFeatureModelFromOtherModel(CustomSVDFeature otherModel,
                                                                       List<String> biasFeas,
                                                                       List<String> ufactFeas,
                                                                       List<String> ifactFeas,
                                                                       String labelName,
                                                                       String weightName,
                                                                       List<String> groupKeys,
                                                                       List<FeatureExtractor> featureExtractors,
                                                                       ObjectiveFunction objectiveFunction) {
        return new CustomSVDFeature(biasFeas, ufactFeas, ifactFeas, labelName, weightName, groupKeys,
                featureExtractors, otherModel.factDim, objectiveFunction, otherModel.indexSpace,
                otherModel.variableSpace);
    }

    /**
     * Directly calling this is discouraged. Use {@link CustomSVDFeatureProducer} instead.
     */
    public CustomSVDFeature(List<String> biasFeas, List<String> ufactFeas, List<String> ifactFeas,
                            String labelName, String weightName, List<String> groupKeys,
                            List<FeatureExtractor> featureExtractors, int factDim,
                            ObjectiveFunction objectiveFunction, IndexSpace indexSpace,
                            VariableSpace variableSpace) {
        super(biasFeas, ufactFeas, ifactFeas, labelName, weightName, groupKeys, featureExtractors, factDim, objectiveFunction, indexSpace, variableSpace);
    }

    // Nonnegative vector initialization.
    @Override
    protected void ensureVectorVarSpace(List<Feature> features) {
        for (Feature fea : features) {
            variableSpace.ensureVectorVar(SVDFeatureKey.FACTORS.get(),
                    fea.getIndex() + 1, factDim,
                    0, true, false);
            RealVector vec = getVectorVarByNameIndex(SVDFeatureKey.FACTORS.get(), fea.getIndex());
            setVectorVarByNameIndex(SVDFeatureKey.FACTORS.get(), fea.getIndex(), vec.map(x -> Math.abs(x)));
        }
    }
}