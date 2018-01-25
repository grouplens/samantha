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

public class SeparatedStringGroupExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private static Logger logger = LoggerFactory.getLogger(SeparatedStringGroupExtractor.class);
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

    public SeparatedStringGroupExtractor(String indexName,
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
            String[] fields = entity.get(attrName).asText().split(separator, -1);
            String[] indices = entity.get(inGrpRankName).asText().split(separator, -1);
            int len = fields.length;
            int numGrp = 0;
            if (len > 0) {
                numGrp = 1;
            }
            int inGrpSize = 0;
            int start = 0;
            if (maxGrpNum != null) {
                int maxGrp = maxGrpNum;
                int prevRank = Integer.MAX_VALUE;
                for (int i = len - 1; i >= 0; i--) {
                    int curRank = Integer.parseInt(indices[i]);
                    if ((inGrpSize >= grpSize) || (curRank >= prevRank && curRank != Integer.MAX_VALUE)) {
                        if (numGrp + 1 > maxGrp) {
                            start = i + 1;
                            break;
                        }
                        numGrp++;
                        inGrpSize = 0;
                    }
                    prevRank = curRank;
                    inGrpSize++;
                }
            }
            double val = 1.0;
            if (numGrp > 0 && normalize) {
                val = 1.0 / Math.sqrt(numGrp);
            }
            int prevRank = Integer.MIN_VALUE;
            inGrpSize = 0;
            numGrp = 0;
            if (start < len) {
                numGrp = 1;
            }
            for (int i=start; i<len; i++) {
                int curRank = Integer.parseInt(indices[i]);
                if ((inGrpSize >= grpSize) || (curRank <= prevRank && curRank != Integer.MIN_VALUE)) {
                    for (int j=0; j<grpSize - inGrpSize; j++) {
                        String key = FeatureExtractorUtilities.composeKey(attrName, "");
                        FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(features, update,
                                indexSpace, indexName, key, val);
                    }
                    numGrp++;
                    inGrpSize = 0;
                }
                String field = fields[i];
                String key = FeatureExtractorUtilities.composeKey(attrName, field);
                FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(features, update,
                        indexSpace, indexName, key, val);
                inGrpSize++;
                prevRank = curRank;
            }
            for (int j=0; j<grpSize - inGrpSize; j++) {
                String key = FeatureExtractorUtilities.composeKey(attrName, "");
                FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(features, update,
                        indexSpace, indexName, key, val);
            }
            feaMap.put(feaName, features);
            if (sizeFeaIndexName != null && sizeFeaName != null) {
                List<Feature> numGrpFeas = new ArrayList<>();
                FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(numGrpFeas, update,
                        indexSpace, sizeFeaIndexName, inGrpRankName, numGrp * grpSize);
                feaMap.put(sizeFeaName, numGrpFeas);
            }
        } else {
            logger.warn("{} or {} is not present in {}", attrName, inGrpRankName, entity);
        }
        return feaMap;
    }
}
