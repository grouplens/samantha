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

public class SeparatedStringExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private static Logger logger = LoggerFactory.getLogger(SeparatedStringExtractor.class);
    private final String indexName;
    private final String attrName;
    private final String feaName;
    private final String separator;
    private final boolean normalize;
    private final String fillIn;
    private final Integer maxFeatures;

    public SeparatedStringExtractor(String indexName,
                                    String attrName,
                                    String feaName,
                                    String separator,
                                    boolean normalize,
                                    String fillIn,
                                    Integer maxFeatures) {
        this.indexName = indexName;
        this.attrName = attrName;
        this.feaName = feaName;
        this.separator = separator;
        this.normalize = normalize;
        this.fillIn = fillIn;
        this.maxFeatures = maxFeatures;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        Map<String, List<Feature>> feaMap = new HashMap<>();
        if (entity.has(attrName) || fillIn != null) {
            List<Feature> features = new ArrayList<>();
            String attr = fillIn;
            if (entity.has(attrName)) {
                attr = entity.get(attrName).asText();
            }
            String[] fields = attr.split(separator, -1);
            int start = 0;
            if (maxFeatures != null && fields.length > maxFeatures) {
                start = fields.length - maxFeatures;
            }
            double val = 1.0;
            if (fields.length > 0 && normalize) {
                val = 1.0 / Math.sqrt(fields.length - start);
            }
            for (int i=start; i<fields.length; i++) {
                String field = fields[i];
                String key = FeatureExtractorUtilities.composeKey(attrName, field);
                FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(features, update,
                        indexSpace, indexName, key, val);
            }
            feaMap.put(feaName, features);
        }
        if (!entity.has(attrName)){
            logger.warn("{} is not present in {}", attrName, entity);
        }
        return feaMap;
    }
}
