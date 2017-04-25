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
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnglishTokenizeExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private static Logger logger = LoggerFactory.getLogger(EnglishTokenizeExtractor.class);
    private final String indexName;
    private final List<String> attrNames;
    private final String feaName;
    private final String vocabularyName;
    private final Analyzer analyzer;
    private final boolean sigmoid;

    public EnglishTokenizeExtractor(String indexName,
                                    List<String> attrNames,
                                    String feaName,
                                    String vocabularyName,
                                    boolean sigmoid) {
        this.indexName = indexName;
        this.attrNames = attrNames;
        this.feaName = feaName;
        this.vocabularyName = vocabularyName;
        this.analyzer = new EnglishAnalyzer();
        this.sigmoid = sigmoid;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        List<String> textList = new ArrayList<>();
        for (String attrName : attrNames) {
            if (entity.has(attrName)) {
                textList.add(entity.get(attrName).asText());
            } else {
                logger.warn("{} is not present in {}", attrName, entity);
            }
        }
        String text = StringUtils.join(textList, ". ");
        Map<String, Integer> termFreq = FeatureExtractorUtilities.getTermFreq(analyzer, text, vocabularyName);
        List<Feature> features = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
            String key = FeatureExtractorUtilities.composeKey(vocabularyName, entry.getKey());
            double value = entry.getValue();
            if (sigmoid) {
                value = new SelfPlusOneRatioFunction().value(value);
            }
            FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(features, update,
                    indexSpace, indexName, key, value / Math.sqrt(termFreq.size()));
        }
        Map<String, List<Feature>> feaMap = new HashMap<>();
        feaMap.put(feaName, features);
        return feaMap;
    }
}
