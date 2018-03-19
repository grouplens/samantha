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
import org.grouplens.samantha.modeler.model.IndexSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeparatedIdentityGroupExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private static Logger logger = LoggerFactory.getLogger(SeparatedIdentityGroupExtractor.class);
    private final String indexName;
    private final String sizeFeaIndexName;
    private final String attrName;
    private final String inGrpRankName;
    private final String feaName;
    private final String sizeFeaName;
    private final String separator;
    private final boolean normalize;
    private final Integer maxGrpNum;
    private final int grpSize;

    public SeparatedIdentityGroupExtractor(String indexName,
                                           String sizeFeaIndexName,
                                           String attrName,
                                           String feaName,
                                           String sizeFeaName,
                                           String separator,
                                           boolean normalize,
                                           Integer maxGrpNum,
                                           int grpSize,
                                           String inGrpRankName) {
        this.indexName = indexName;
        this.sizeFeaIndexName = sizeFeaIndexName;
        this.attrName = attrName;
        this.feaName = feaName;
        this.sizeFeaName = sizeFeaName;
        this.separator = separator;
        this.normalize = normalize;
        this.maxGrpNum = maxGrpNum;
        this.grpSize = grpSize;
        this.inGrpRankName = inGrpRankName;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        Map<String, List<Feature>> feaMap = new HashMap<>();
        if (entity.has(attrName) && entity.has(inGrpRankName)) {
            List<Feature> features = new ArrayList<>();
            String attrStr = entity.get(attrName).asText();
            int start = 0;
            int numGrp = 0;
            String[] fields = {};
            String[] indices = {};
            if (!"".equals(attrStr)) {
                fields = attrStr.split(separator, -1);
                indices = entity.get(inGrpRankName).asText().split(separator, -1);
                Map.Entry<Integer, Integer> entry = FeatureExtractorUtilities.getStartAndNumGroup(
                        indices, maxGrpNum, grpSize);
                start = entry.getKey();
                numGrp = entry.getValue();
            }
            double val = 0.0;
            int prevRank = Integer.MIN_VALUE;
            int inGrpSize = 0;
            int len = fields.length;
            for (int i=start; i<len; i++) {
                int curRank = Integer.parseInt(indices[i]);
                if ((inGrpSize >= grpSize) || (curRank <= prevRank && curRank != Integer.MIN_VALUE)) {
                    for (int j=0; j<grpSize - inGrpSize; j++) {
                        FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(features, update,
                                indexSpace, indexName, attrName, val);
                    }
                    inGrpSize = 0;
                }
                val = Double.parseDouble(fields[i]);
                if (numGrp > 0 && normalize) {
                    val /= numGrp;
                }
                FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(features, update,
                        indexSpace, indexName, attrName, val);
                inGrpSize++;
                prevRank = curRank;
            }
            if (numGrp > 0) {
                for (int j = 0; j < grpSize - inGrpSize; j++) {
                    FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(features, update,
                            indexSpace, indexName, attrName, val);
                }
            }
            feaMap.put(feaName, features);
            if (sizeFeaIndexName != null && sizeFeaName != null) {
                List<Feature> numGrpFeas = new ArrayList<>();
                FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(numGrpFeas, update,
                        indexSpace, sizeFeaIndexName, inGrpRankName, features.size());
                feaMap.put(sizeFeaName, numGrpFeas);
            }
        } else {
            logger.warn("{} or {} is not present in {}", attrName, inGrpRankName, entity);
        }
        return feaMap;
    }
}
