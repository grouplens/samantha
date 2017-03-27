package org.grouplens.samantha.ephemeral.model;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;

import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.featurizer.Feature;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.modeler.featurizer.Featurizer;
import org.grouplens.samantha.modeler.solver.*;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureInstance;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureKey;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.exception.ConfigurationException;
import org.grouplens.samantha.server.retriever.RetrieverUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.*;
import java.util.stream.Collectors;

public class CustomSVDFeature extends AbstractLearningModel implements Featurizer {
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(CustomSVDFeature.class);

    private final ObjectiveFunction objectiveFunction;
    private final List<String> biasFeas = new ArrayList<>();
    private final List<String> ufactFeas = new ArrayList<>();
    private final List<String> ifactFeas = new ArrayList<>();
    private final List<String> groupKeys;
    private final String labelName;
    private final String weightName;
    private final List<FeatureExtractor> featureExtractors = new ArrayList<>();
    private final int factDim;

    // Cache the average user vector so that we only need to calculate it once
    // after we're done building the model. DUMP should serialize it to disk,
    // along with the rest of the model.
    private RealVector averageUserVector;
    public RealVector getAverageUserVector() {
        if (averageUserVector == null) {
            throw new IllegalStateException("model not trained");
        }
        // Don't allow other classes to (accidentally) modify the vector
        return RealVector.unmodifiableRealVector(averageUserVector);
    }
    public void calculateAverageUserVector() {
        List<Integer> indices = new ArrayList<>();

        int size = getKeyMapSize(SVDFeatureKey.FACTORS.get());
        for (int i = 0; i < size; i++) {
            String key = (String) getKeyForIndex(SVDFeatureKey.FACTORS.get(), i);
            if (key.startsWith("userId")) {
                indices.add(i);
            }
        }

        // Model hasn't been trained yet...
        if (indices.isEmpty()) {
            throw new IllegalStateException("no user vectors found in model");
        }

        averageUserVector = indices.stream()
                .map(index -> getVectorVarByNameIndex(SVDFeatureKey.FACTORS.get(), index))
                .reduce((v1, v2) -> v1.add(v2))
                .get().mapDivide(indices.size());

        Format d = new DecimalFormat("#.###");
        logger.info("calculated average of {} user vectors, norm={}, elements=[{}]",
                indices.size(),
                d.format(averageUserVector.getNorm()),
                realVectorToString(averageUserVector));
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
    public CustomSVDFeature(List<String> biasFeas,
                      List<String> ufactFeas,
                      List<String> ifactFeas,
                      String labelName,
                      String weightName,
                      List<String> groupKeys,
                      List<FeatureExtractor> featureExtractors,
                      int factDim,
                      ObjectiveFunction objectiveFunction,
                      IndexSpace indexSpace,
                      VariableSpace variableSpace) {
        super(indexSpace, variableSpace);
        this.factDim = factDim;
        this.biasFeas.addAll(biasFeas);
        this.ufactFeas.addAll(ufactFeas);
        this.ifactFeas.addAll(ifactFeas);
        this.labelName = labelName;
        this.weightName = weightName;
        this.groupKeys = groupKeys;
        this.featureExtractors.addAll(featureExtractors);
        this.objectiveFunction = objectiveFunction;
    }

    public String getLabelName() {
        return labelName;
    }

    public String getWeightName() {
        return weightName;
    }

    public List<String> getAllScalarVarNames() {
        List<String> names = new ArrayList<>();
        names.add(SVDFeatureKey.BIASES.get());
        return names;
    }

    public List<String> getAllVectorVarNames() {
        List<String> names = new ArrayList<>();
        names.add(SVDFeatureKey.FACTORS.get());
        return names;
    }

    public Object2DoubleMap<String> getFactorFeatures(int minSupport) {
        int numFeas = indexSpace.getKeyMapSize(SVDFeatureKey.FACTORS.get());
        Object2DoubleMap<String> fea2sup = new Object2DoubleOpenHashMap<>();
        for (int i=0; i<numFeas; i++) {
            String feature = (String)indexSpace.getKeyForIndex(SVDFeatureKey.FACTORS.get(), i);
            if (indexSpace.containsKey(SVDFeatureKey.BIASES.get(), feature)) {
                int idx = indexSpace.getIndexForKey(SVDFeatureKey.BIASES.get(), feature);
                double support = variableSpace.getScalarVarByNameIndex(SVDFeatureKey.SUPPORT.get(), idx);
                if (support >= minSupport) {
                    fea2sup.put(feature, support);
                }
            }
        }
        return fea2sup;
    }

    private List<Feature> getFeatures(List<String> feaNames, Map<String, List<Feature>> feaMap) {
        List<Feature> feaList = new ArrayList<>();
        for (String feaName : feaNames) {
            if (feaMap.containsKey(feaName)) {
                feaList.addAll(feaMap.get(feaName));
            }
        }
        return feaList;
    }

    private void ensureScalarVarSpace(List<Feature> features) {
        for (Feature fea : features) {
            variableSpace.ensureScalarVar(SVDFeatureKey.BIASES.get(),
                    fea.getIndex() + 1, 0, false);
            variableSpace.ensureScalarVar(SVDFeatureKey.SUPPORT.get(),
                    fea.getIndex() + 1, 0, false);
        }
    }

    // Updated to ensure nonnegative initial conditions.
    private void ensureVectorVarSpace(List<Feature> features) {
        for (Feature fea : features) {
            variableSpace.ensureVectorVar(SVDFeatureKey.FACTORS.get(),
                    fea.getIndex() + 1, factDim,
                    0, true, false);
            RealVector vec = getVectorVarByNameIndex(SVDFeatureKey.FACTORS.get(), fea.getIndex());
            setVectorVarByNameIndex(SVDFeatureKey.FACTORS.get(), fea.getIndex(), vec.map(x -> Math.abs(x)));
        }
    }

    private String realVectorToString(RealVector vec) {
        String[] arr = new String[vec.getDimension()];
        for (int i=0; i<vec.getDimension(); i++) {
            arr[i] = Double.valueOf(vec.getEntry(i)).toString();
        }
        return StringUtils.join(arr, "\t");
    }

    private void updateFeatureSupport(List<Feature> gfeas) {
        synchronized (variableSpace) {
            for (Feature fea : gfeas) {
                double support = variableSpace.getScalarVarByNameIndex(SVDFeatureKey.SUPPORT.get(), fea.getIndex());
                variableSpace.setScalarVarByNameIndex(SVDFeatureKey.SUPPORT.get(), fea.getIndex(), support + 1.0);
            }
        }
    }

    public Map<String, List<Feature>> getFeatureMap(JsonNode entity, boolean update) {
        Map<String, List<Feature>> feaMap = new HashMap<>();
        for (FeatureExtractor extractor : featureExtractors) {
            feaMap.putAll(extractor.extract(entity, update,
                    indexSpace));
        }
        return feaMap;
    }

    public LearningInstance featurize(JsonNode entity, boolean update) {
        Map<String, List<Feature>> feaMap = getFeatureMap(entity, update);
        List<Feature> gfeas = getFeatures(biasFeas, feaMap);
        List<Feature> ufeas = getFeatures(ufactFeas, feaMap);
        List<Feature> ifeas = getFeatures(ifactFeas, feaMap);
        if (update) {
            ensureScalarVarSpace(gfeas);
            updateFeatureSupport(gfeas);
            ensureVectorVarSpace(ufeas);
            ensureVectorVarSpace(ifeas);
        }
        double weight = SVDFeatureInstance.defaultWeight;
        double label = SVDFeatureInstance.defaultLabel;
        if (entity.has(labelName)) {
            label = entity.get(labelName).asDouble();
        } else if (feaMap.containsKey(labelName)) {
            label = feaMap.get(labelName).get(0).getValue();
        }
        if (entity.has(weightName)) {
            weight = entity.get(weightName).asDouble();
        } else if (feaMap.containsKey(weightName)) {
            weight = feaMap.get(weightName).get(0).getValue();
        }
        String group = null;
        if (groupKeys != null && groupKeys.size() > 0) {
            group = FeatureExtractorUtilities.composeConcatenatedKey(entity, groupKeys);
        }
        return new SVDFeatureInstance(gfeas, ufeas, ifeas, label, weight, group);
    }

    public ObjectiveFunction getObjectiveFunction() {
        return objectiveFunction;
    }

    private double predict(SVDFeatureInstance ins, StochasticOracle outOrc,
                           RealVector outUfactSum, RealVector outIfactSum) {
        double pred = 0.0;
        for (int i=0; i<ins.getBiasFeatures().size(); i++) {
            int ind = ins.getBiasFeatures().get(i).getIndex();
            double val = ins.getBiasFeatures().get(i).getValue();
            if (outOrc != null) {
                outOrc.addScalarOracle(SVDFeatureKey.BIASES.get(), ind, val);
            }
            pred += getScalarVarByNameIndex(SVDFeatureKey.BIASES.get(), ind) * val;
        }

        outUfactSum.set(0.0);
        for (int i=0; i<ins.getUserFeatures().size(); i++) {
            int index = ins.getUserFeatures().get(i).getIndex();
            outUfactSum.combineToSelf(1.0, ins.getUserFeatures().get(i).getValue(),
                    getVectorVarByNameIndex(SVDFeatureKey.FACTORS.get(), index));
        }

        outIfactSum.set(0.0);
        for (int i=0; i<ins.getItemFeatures().size(); i++) {
            int index = ins.getItemFeatures().get(i).getIndex();
            outIfactSum.combineToSelf(1.0, ins.getItemFeatures().get(i).getValue(),
                    getVectorVarByNameIndex(SVDFeatureKey.FACTORS.get(), index));
        }

        pred += outUfactSum.dotProduct(outIfactSum);
        return pred;
    }

    public List<StochasticOracle> getStochasticOracle(List<LearningInstance> instances) {
        List<StochasticOracle> oracles = new ArrayList<>(instances.size());
        for (LearningInstance inIns : instances) {
            SVDFeatureInstance ins = (SVDFeatureInstance) inIns;
            StochasticOracle orc = new StochasticOracle();
            RealVector ufactSum = MatrixUtils.createRealVector(new double[factDim]);
            RealVector ifactSum = MatrixUtils.createRealVector(new double[factDim]);
            double pred = predict(ins, orc, ufactSum, ifactSum);
            RealVector leftGrad = ifactSum;
            RealVector rightGrad = ufactSum;
            for (int i = 0; i < ins.getUserFeatures().size(); i++) {
                orc.addVectorOracle(SVDFeatureKey.FACTORS.get(),
                        ins.getUserFeatures().get(i).getIndex(),
                        leftGrad.mapMultiply(ins.getUserFeatures().get(i).getValue()));
            }
            for (int i = 0; i < ins.getItemFeatures().size(); i++) {
                orc.addVectorOracle(SVDFeatureKey.FACTORS.get(),
                        ins.getItemFeatures().get(i).getIndex(),
                        rightGrad.mapMultiply(ins.getItemFeatures().get(i).getValue()));
            }
            orc.setValues(pred, ins.getLabel(), ins.getWeight());
            oracles.add(orc);
        }
        return oracles;
    }

    private List<Feature> ensureMinSupport(List<Feature> feas, boolean bias) {
        double minSupport = 10;
        List<Feature> nfeas = new ArrayList<>();
        for (Feature fea : feas) {
            int idx = fea.getIndex();
            if (!bias) {
                Object feature = indexSpace.getKeyForIndex(SVDFeatureKey.FACTORS.get(), idx);
                idx = indexSpace.getIndexForKey(SVDFeatureKey.BIASES.get(), feature);

            }
            double support = variableSpace.getScalarVarByNameIndex(SVDFeatureKey.SUPPORT.get(), idx);
            if (support >= minSupport) {
                nfeas.add(fea);
            }
        }
        return nfeas;
    }

    public double predict(LearningInstance ins) {
        SVDFeatureInstance svdIns = (SVDFeatureInstance) ins;
        RealVector ufactSum = MatrixUtils.createRealVector(new double[factDim]);
        RealVector ifactSum = MatrixUtils.createRealVector(new double[factDim]);
        double output = predict(svdIns, null, ufactSum, ifactSum);
        return objectiveFunction.wrapOutput(output);
    }

}
