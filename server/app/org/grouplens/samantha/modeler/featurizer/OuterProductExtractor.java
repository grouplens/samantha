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
import com.google.common.collect.Lists;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Log10;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OuterProductExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private static Logger logger = LoggerFactory.getLogger(OuterProductExtractor.class);
    private final String indexName;
    private final List<String> attrNames;
    private final String feaName;
    private final boolean sigmoid;

    public OuterProductExtractor(String indexName, List<String> attrNames, String feaName, boolean sigmoid) {
        this.feaName = feaName;
        this.indexName = indexName;
        this.attrNames = attrNames;
        this.sigmoid = sigmoid;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        Map<String, List<Feature>> feaMap = new HashMap<>();
        List<Feature> feaList = new ArrayList<>();
        for (String attrName : attrNames) {
            if (!entity.has(attrName)) {
                logger.warn("{} is not present in {}", attrName, entity);
            }
        }
        for (String leftName : attrNames) {
            for (String rightName : attrNames) {
                List<String> keyNames = Lists.newArrayList(leftName, rightName);
                String key = FeatureExtractorUtilities.composeKey(keyNames);
                double product;
                if (sigmoid) {
                    UnivariateFunction func = new SelfPlusOneRatioFunction();
                    product = func.value(entity.get(leftName).asDouble()) *
                            func.value(entity.get(rightName).asDouble());
                } else {
                    product = entity.get(leftName).asDouble() * entity.get(rightName).asDouble();
                }
                FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(feaList, update,
                        indexSpace, indexName, key, product);
                feaMap.put(feaName, feaList);
            }
        }
        return feaMap;
    }
}
