package org.grouplens.samantha.modeler.svdfeature;

import com.fasterxml.jackson.databind.JsonNode;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.apache.commons.lang3.StringUtils;
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
import org.grouplens.samantha.server.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class SVDFeature extends AbstractLearningModel implements Featurizer {
    private static final long serialVersionUID = 1L;
    private static Logger logger = LoggerFactory.getLogger(SVDFeature.class);

    private final ObjectiveFunction objectiveFunction;
    private final List<String> biasFeas = new ArrayList<>();
    private final List<String> ufactFeas = new ArrayList<>();
    private final List<String> ifactFeas = new ArrayList<>();
    private final List<String> groupKeys;
    private final String labelName;
    private final String weightName;
    private final List<FeatureExtractor> featureExtractors = new ArrayList<>();
    private final int factDim;

    static public SVDFeature createSVDFeatureModelFromOtherModel(SVDFeature otherModel,
                                                                 List<String> biasFeas,
                                                                 List<String> ufactFeas,
                                                                 List<String> ifactFeas,
                                                                 String labelName,
                                                                 String weightName,
                                                                 List<String> groupKeys,
                                                                 List<FeatureExtractor> featureExtractors,
                                                                 ObjectiveFunction objectiveFunction) {
        return new SVDFeature(biasFeas, ufactFeas, ifactFeas, labelName, weightName, groupKeys,
                featureExtractors, otherModel.factDim, objectiveFunction, otherModel.indexSpace,
                otherModel.variableSpace);
    }

    /**
     * Directly calling this is discouraged. Use {@link SVDFeatureProducer} instead.
     */
    public SVDFeature(List<String> biasFeas,
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

    private void ensureVectorVarSpace(List<Feature> features) {
        for (Feature fea : features) {
            variableSpace.ensureVectorVar(SVDFeatureKey.FACTORS.get(),
                                          fea.getIndex() + 1, factDim,
                                          0, true, false);
        }
    }

    private String realVectorToString(RealVector vec) {
        String[] arr = new String[vec.getDimension()];
        for (int i=0; i<vec.getDimension(); i++) {
            arr[i] = Double.valueOf(vec.getEntry(i)).toString();
        }
        return StringUtils.join(arr, "\t");
    }

    public void dump(File modelFile) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(modelFile));
            RealVector biases = variableSpace.getScalarVarByName(SVDFeatureKey.BIASES.get());
            writer.write(Double.valueOf(biases.getEntry(0)).toString() + "\n");
            String biasLine = realVectorToString(biases.getSubVector(1, biases.getDimension() - 1));
            writer.write(biasLine + "\n");
            List<RealVector> factors = variableSpace.getVectorVarByName(SVDFeatureKey.FACTORS.get());
            for (int i=0; i<factors.size(); i++) {
                String factLine = realVectorToString(factors.get(i));
                writer.write(factLine + "\n");
            }
            writer.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new BadRequestException(e);
        }
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
        for (int i=0; i<ins.gfeas.size(); i++) {
            int ind = ins.gfeas.get(i).getIndex();
            double val = ins.gfeas.get(i).getValue();
            if (outOrc != null) {
                outOrc.addScalarOracle(SVDFeatureKey.BIASES.get(), ind, val);
            }
            pred += getScalarVarByNameIndex(SVDFeatureKey.BIASES.get(), ind) * val;
        }

        outUfactSum.set(0.0);
        for (int i=0; i<ins.ufeas.size(); i++) {
            int index = ins.ufeas.get(i).getIndex();
            outUfactSum.combineToSelf(1.0, ins.ufeas.get(i).getValue(),
                                      getVectorVarByNameIndex(SVDFeatureKey.FACTORS.get(), index));
        }

        outIfactSum.set(0.0);
        for (int i=0; i<ins.ifeas.size(); i++) {
            int index = ins.ifeas.get(i).getIndex();
            outIfactSum.combineToSelf(1.0, ins.ifeas.get(i).getValue(),
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
            for (int i = 0; i < ins.ufeas.size(); i++) {
                orc.addVectorOracle(SVDFeatureKey.FACTORS.get(),
                        ins.ufeas.get(i).getIndex(),
                        leftGrad.mapMultiply(ins.ufeas.get(i).getValue()));
            }
            for (int i = 0; i < ins.ifeas.size(); i++) {
                orc.addVectorOracle(SVDFeatureKey.FACTORS.get(),
                        ins.ifeas.get(i).getIndex(),
                        rightGrad.mapMultiply(ins.ifeas.get(i).getValue()));
            }
            orc.setValues(pred, ins.label, ins.weight);
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
        svdIns.gfeas = ensureMinSupport(svdIns.gfeas, true);
        svdIns.ufeas = ensureMinSupport(svdIns.ufeas, false);
        svdIns.ifeas = ensureMinSupport(svdIns.ifeas, false);
        RealVector ufactSum = MatrixUtils.createRealVector(new double[factDim]);
        RealVector ifactSum = MatrixUtils.createRealVector(new double[factDim]);
        double output = predict(svdIns, null, ufactSum, ifactSum);
        return objectiveFunction.wrapOutput(output);
    }
}
