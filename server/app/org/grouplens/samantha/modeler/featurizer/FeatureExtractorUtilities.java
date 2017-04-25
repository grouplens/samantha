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
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Map<String, Integer> termFreq = new HashMap<>();
        try {
            ts.reset();
            while (ts.incrementToken()) {
                String term = ts.reflectAsString(false);
                int cnt = termFreq.getOrDefault(term, 0);
                termFreq.put(term, cnt + 1);
            }
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
        String[] multiples = key.split("\t");
        for (String multiple : multiples) {
            String[] attrVal = multiple.split("\1");
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
}
