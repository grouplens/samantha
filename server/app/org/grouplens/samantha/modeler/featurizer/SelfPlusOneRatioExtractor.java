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
import org.apache.commons.math3.analysis.function.Log10;
import org.grouplens.samantha.modeler.model.IndexSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.abs;

public class SelfPlusOneRatioExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private static Logger logger = LoggerFactory.getLogger(SelfPlusOneRatioExtractor.class);
    private final String indexName;
    private final String attrName;
    private final String feaName;
    private final boolean sparse;
    private final boolean log;

    public SelfPlusOneRatioExtractor(String indexName, String attrName, String feaName,
                                     boolean sparse, boolean log) {
        this.attrName = attrName;
        this.feaName = feaName;
        this.indexName = indexName;
        this.sparse = sparse;
        this.log = log;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        Map<String, List<Feature>> feaMap = new HashMap<>();
        if (entity.has(attrName)) {
            SelfPlusOneRatioFunction ratio = new SelfPlusOneRatioFunction();
            List<Feature> feaList = new ArrayList<>();
            double val = entity.get(attrName).asDouble();
            if (!sparse || val != 0.0) {
                if (log) {
                    Log10 log10 = new Log10();
                    val = log10.value(val + 1.0);
                }
                FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(feaList, update,
                        indexSpace, indexName, attrName, ratio.value(val));
                feaMap.put(feaName, feaList);
            }
        } else {
            logger.warn("{} is not present in {}", attrName, entity);
        }
        return feaMap;
    }
}
