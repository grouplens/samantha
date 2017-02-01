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
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.grouplens.samantha.modeler.space.IndexSpace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiplicativeInteractionExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private final String indexName;
    private final List<String> attrNames;
    private final String feaName;
    private final boolean sigmoid;

    public MultiplicativeInteractionExtractor(String indexName, List<String> attrNames,
                                              String feaName, boolean sigmoid) {
        this.feaName = feaName;
        this.indexName = indexName;
        this.attrNames = attrNames;
        this.sigmoid = sigmoid;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        Map<String, List<Feature>> feaMap = new HashMap<>();
        double product = 1.0;
        boolean complete = true;
        UnivariateFunction sig = new SelfPlusOneRatioFunction();
        for (String attrName : attrNames) {
            if (entity.has(attrName)) {
                double val = entity.get(attrName).asDouble();
                if (sigmoid) {
                    val = sig.value(val);
                }
                product *= val;
            } else {
                complete = false;
            }
        }
        if (complete) {
            List<Feature> feaList = new ArrayList<>();
            String key = FeatureExtractorUtilities.composeKey(attrNames);
            FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(feaList, update,
                    indexSpace, indexName, key, product);
            feaMap.put(feaName, feaList);
        }
        return feaMap;
    }
}
