/*
 * Copyright (c) [2016-2018] [University of Minnesota]
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
import org.grouplens.samantha.modeler.model.IndexSpace;
import org.grouplens.samantha.modeler.tensorflow.TensorFlowModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisplayActionGroupExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private static Logger logger = LoggerFactory.getLogger(DisplayActionGroupExtractor.class);
    private final String inGrpRank;
    private final String index;
    private final String attr;
    private final String fea;
    private final String sizeFeaIndex;
    private final String sizeFea;
    private final List<String> actionIndices;
    private final List<String> actionAttrs;
    private final List<String> actionFeas;
    private final List<Boolean> extractBools;
    private final String displayActionIndex;
    private final String displayActionFea;
    private final String separator;
    private final boolean normalize;
    private final Integer maxGrpNum;
    private final int grpSize;

    public DisplayActionGroupExtractor(String index,
                                       String sizeFeaIndex,
                                       String attr,
                                       String fea,
                                       String sizeFea,
                                       List<String> actionIndices,
                                       List<String> actionAttrs,
                                       List<String> actionFeas,
                                       List<Boolean> extractBools,
                                       String displayActionIndex,
                                       String displayActionFea,
                                       String separator,
                                       boolean normalize,
                                       Integer maxGrpNum,
                                       int grpSize,
                                       String inGrpRank) {
        this.index = index;
        this.sizeFeaIndex = sizeFeaIndex;
        this.sizeFea = sizeFea;
        this.actionAttrs = actionAttrs;
        this.actionFeas = actionFeas;
        this.actionIndices = actionIndices;
        this.extractBools = extractBools;
        this.displayActionFea = displayActionFea;
        this.displayActionIndex = displayActionIndex;
        this.attr = attr;
        this.fea = fea;
        this.separator = separator;
        this.normalize = normalize;
        this.maxGrpNum = maxGrpNum;
        this.grpSize = grpSize;
        this.inGrpRank = inGrpRank;
    }

    private void processFeature(IndexSpace indexSpace, boolean update, List<Feature> features,
                                String indexName, String attrName, String field, double val) {
        String key = TensorFlowModel.OOV;
        if (field != null) {
            key = FeatureExtractorUtilities.composeKey(attrName, field);
        }
        FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(features, update,
                indexSpace, indexName, key, val);
    }

    private void addFillings(int inGrpSize, IndexSpace indexSpace, boolean update,
                             List<Feature> features, List<Feature> disActFeas,
                             Map<String, List<Feature>> act2feas,
                             Map<String, List<Feature>> act2bfeas, double val) {
        for (int j=0; j<grpSize - inGrpSize; j++) {
            processFeature(indexSpace, update, features, index, attr, null, val);
            if (displayActionIndex != null) {
                processFeature(indexSpace, update, disActFeas, displayActionIndex,
                        attr, null, val);
            }
            for (int k=0; k<actionAttrs.size(); k++) {
                String act = actionAttrs.get(k);
                processFeature(indexSpace, update, act2feas.get(act),
                        actionIndices.get(k), act, null, val);
                if (extractBools.get(k)) {
                    processFeature(indexSpace, update, act2bfeas.get(act),
                            actionIndices.get(k) + "_BOOL", act, null, val);
                }
            }
        }
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        Map<String, List<Feature>> feaMap = new HashMap<>();
        boolean hasRequired = entity.has(attr) && entity.has(inGrpRank);
        for (String act : actionAttrs) {
            if (!entity.has(act)) {
                hasRequired = false;
            }
        }
        if (hasRequired) {
            Map<String, String[]> act2bools = new HashMap<>();
            Map<String, List<Feature>> act2feas = new HashMap<>();
            Map<String, List<Feature>> act2bfeas = new HashMap<>();
            for (String act : actionAttrs) {
                act2bools.put(act, entity.get(act).asText().split(separator, -1));
                act2feas.put(act, new ArrayList<>());
                act2bfeas.put(act, new ArrayList<>());
            }
            String attrStr = entity.get(attr).asText();
            int start = 0;
            String[] fields = {};
            String[] indices = {};
            double val = 1.0;
            if (!"".equals(attrStr)) {
                indices = entity.get(inGrpRank).asText().split(separator, -1);
                Map.Entry<Integer, Integer> entry = FeatureExtractorUtilities.getStartAndNumGroup(
                        indices, maxGrpNum, grpSize);
                start = entry.getKey();
                int numGrp = entry.getValue();
                fields = attrStr.split(separator, -1);
                if (numGrp > 0 && normalize) {
                    val = 1.0 / Math.sqrt(numGrp);
                }
            }
            int prevRank = Integer.MIN_VALUE;
            int inGrpSize = 0;
            int len = fields.length;
            List<Feature> features = new ArrayList<>();
            List<Feature> disActFeas = new ArrayList<>();
            for (int i=start; i<len; i++) {
                int curRank = Integer.parseInt(indices[i]);
                if ((inGrpSize >= grpSize) || (curRank <= prevRank && curRank != Integer.MIN_VALUE)) {
                    addFillings(inGrpSize, indexSpace, update, features, disActFeas,
                            act2feas, act2bfeas, val);
                    inGrpSize = 0;
                }
                processFeature(indexSpace, update, features, index, attr, fields[i], val);
                String disAct = "";
                for (int j=0; j<actionAttrs.size(); j++) {
                    String act = actionAttrs.get(j);
                    String bactStr = act2bools.get(act)[i];
                    double bactVal = Double.parseDouble(bactStr);
                    if (bactVal > 0.0) {
                        processFeature(indexSpace, update, act2feas.get(act),
                                actionIndices.get(j), act, fields[i], val);
                        disAct = act;
                        bactStr = "True";
                    } else {
                        processFeature(indexSpace, update, act2feas.get(act),
                                actionIndices.get(j), act, null, val);
                        bactStr = "False";
                    }
                    if (extractBools.get(j)) {
                        processFeature(indexSpace, update, act2bfeas.get(act),
                                actionIndices.get(j) + "_BOOL", act,
                                fields[i] + "_" + bactStr, val);
                    }
                }
                if (displayActionIndex != null) {
                    processFeature(indexSpace, update, disActFeas, displayActionIndex,
                            attr, disAct + fields[i], val);
                }
                inGrpSize++;
                prevRank = curRank;
            }
            if (len > 0) {
                addFillings(inGrpSize, indexSpace, update, features, disActFeas,
                        act2feas, act2bfeas, val);
            }
            feaMap.put(fea, features);
            if (displayActionFea != null) {
                feaMap.put(displayActionFea, disActFeas);
            }
            for (int i=0; i<actionAttrs.size(); i++) {
                String actFea = actionFeas.get(i);
                feaMap.put(actFea, act2feas.get(actionAttrs.get(i)));
                if (extractBools.get(i)) {
                    feaMap.put("b" + actFea, act2bfeas.get(actionAttrs.get(i)));
                }
            }
            if (sizeFeaIndex != null && sizeFea != null) {
                List<Feature> numGrpFeas = new ArrayList<>();
                FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(numGrpFeas, update,
                        indexSpace, sizeFeaIndex, inGrpRank, features.size());
                feaMap.put(sizeFea, numGrpFeas);
            }
        } else {
            logger.warn("{} or {} or {} is not present in {}", attr, inGrpRank, actionAttrs, entity);
        }
        return feaMap;
    }
}
