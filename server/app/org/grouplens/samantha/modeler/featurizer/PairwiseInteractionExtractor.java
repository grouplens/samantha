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
import org.grouplens.samantha.modeler.model.IndexSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PairwiseInteractionExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private static Logger logger = LoggerFactory.getLogger(PairwiseInteractionExtractor.class);
    private final String indexName;
    private final List<String> attrNames;
    private final boolean sigmoid;

    public PairwiseInteractionExtractor(String indexName, List<String> attrNames, boolean sigmoid) {
        this.indexName = indexName;
        this.attrNames = attrNames;
        this.sigmoid = sigmoid;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        for (String attrName : attrNames) {
            if (!entity.has(attrName)) {
                logger.warn("{} is not present in {}", attrName, entity);
            }
        }
        Map<String, List<Feature>> feaMap = new HashMap<>();
        UnivariateFunction sig = new SelfPlusOneRatioFunction();
        for (int i=0; i<attrNames.size(); i++) {
            String attrNameLeft = attrNames.get(i);
            for (int j=i+1; j<attrNames.size(); j++) {
                String attrNameRight = attrNames.get(j);
                if (entity.has(attrNameLeft) && entity.has(attrNameRight)) {
                    double valLeft = entity.get(attrNameLeft).asDouble();
                    double valRight = entity.get(attrNameRight).asDouble();
                    if (sigmoid) {
                        valLeft = sig.value(valLeft);
                        valRight = sig.value(valRight);
                    }
                    double value = valLeft * valRight;
                    List<Feature> feaList = new ArrayList<>();
                    String key = FeatureExtractorUtilities.composeKey(attrNameLeft, attrNameRight);
                    FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(feaList, update,
                            indexSpace, indexName, key, value);
                    feaMap.put(attrNameLeft + ":" + attrNameRight, feaList);
                }
            }
        }
        return feaMap;
    }
}
