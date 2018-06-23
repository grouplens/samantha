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
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.grouplens.samantha.modeler.model.IndexSpace;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class FeatureExtractorUtilities {
    private static Logger logger = LoggerFactory.getLogger(FeatureExtractorUtilities.class);

    private FeatureExtractorUtilities() {}

    static public void getOrSetIndexSpaceToFeaturize(List<Feature> features,
                                                     boolean update,
                                                     IndexSpace indexSpace,
                                                     String indexName, Object key,
                                                     double val) {
        if (indexSpace.containsKey(indexName, key)) {
            Feature feature = new Feature(indexSpace.getIndexForKey(indexName, key), val);
            features.add(feature);
        } else if (update) {
            int index = indexSpace.setKey(indexName, key);
            Feature feature = new Feature(index, val);
            features.add(feature);
        }
    }

    static public Map<String, Integer> getTermFreq(Analyzer analyzer, String text, String termField) {
        TokenStream ts = analyzer.tokenStream(termField, text);
        CharTermAttribute cattr = ts.addAttribute(CharTermAttribute.class);
        Map<String, Integer> termFreq = new HashMap<>();
        try {
            ts.reset();
            while (ts.incrementToken()) {
                String term = cattr.toString();
                int cnt = termFreq.getOrDefault(
                        FeatureExtractorUtilities.composeKey(termField, term), 0);
                termFreq.put(term, cnt + 1);
            }
            ts.end();
            ts.close();
        } catch (IOException e) {
            logger.error("{}", e.getMessage());
            throw new BadRequestException(e);
        }
        return termFreq;
    }

    static public String composeKey(String attr, String value) {
        return attr + "\1" + value;
    }

    static public String composeKey(List<String> multiples) {
        return StringUtils.join(multiples.iterator(), "\t");
    }

    static public Map<String, String> decomposeKey(String key) {
        Map<String, String> attrVals = new HashMap<>();
        String[] multiples = key.split("\t", -1);
        for (String multiple : multiples) {
            String[] attrVal = multiple.split("\1", -1);
            if (attrVal.length == 2) {
                attrVals.put(attrVal[0], attrVal[1]);
            }
        }
        return attrVals;
    }

    static public String composeConcatenatedKey(JsonNode entity, List<String> attrNames) {
        List<String> multiples = new ArrayList<>();
        for (String attrName : attrNames) {
            if (entity.has(attrName)) {
                multiples.add(FeatureExtractorUtilities.composeKey(attrName, entity.get(attrName).asText()));
            }
        }
        return composeKey(multiples);
    }

    static public String composeConcatenatedKeyWithoutName(JsonNode entity, List<String> attrNames) {
        List<String> multiples = new ArrayList<>();
        for (String attrName : attrNames) {
            if (entity.has(attrName)) {
                multiples.add(entity.get(attrName).asText());
            }
        }
        return composeKey(multiples);
    }

    static public Map.Entry<Integer, Integer> getStartAndNumGroup(String[] indices, Integer maxGrpNum, int grpSize) {
        int len = indices.length;
        int numGrp = 0;
        if (len > 0) {
            numGrp = 1;
        }
        int inGrpSize = 0;
        int start = 0;
        int prevRank = Integer.MAX_VALUE;
        for (int i = len - 1; i >= 0; i--) {
            int curRank = Integer.parseInt(indices[i]);
            if ((inGrpSize >= grpSize) || (curRank >= prevRank && curRank != Integer.MAX_VALUE)) {
                if (maxGrpNum != null && numGrp + 1 > maxGrpNum) {
                    start = i + 1;
                    break;
                }
                numGrp++;
                inGrpSize = 0;
            }
            prevRank = curRank;
            inGrpSize++;
        }
        return new AbstractMap.SimpleEntry<>(start, numGrp);
    }

    static public int getForwardEnd(String[] indices, Integer maxGrpNum, int grpSize) {
        int len = indices.length;
        if (maxGrpNum == null) {
            return len;
        }
        int numGrp = 0;
        if (len > 0) {
            numGrp = 1;
        }
        int inGrpSize = 0;
        int end = 0;
        int prevRank = Integer.MIN_VALUE;
        for (int i = 0; i < len; i++) {
            int curRank = Integer.parseInt(indices[i]);
            if ((inGrpSize >= grpSize) || (curRank <= prevRank && curRank != Integer.MIN_VALUE)) {
                if (numGrp + 1 > maxGrpNum) {
                    end = i;
                    break;
                }
                numGrp++;
                inGrpSize = 0;
            }
            prevRank = curRank;
            inGrpSize++;
        }
        return end;
    }
}
