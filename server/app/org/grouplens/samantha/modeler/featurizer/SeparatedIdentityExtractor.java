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
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeparatedIdentityExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private static Logger logger = LoggerFactory.getLogger(SeparatedIdentityExtractor.class);
    private final String indexName;
    private final String attrName;
    private final String feaName;
    private final String separator;

    public SeparatedIdentityExtractor(String indexName,
                                    String attrName,
                                    String feaName,
                                    String separator) {
        this.indexName = indexName;
        this.attrName = attrName;
        this.feaName = feaName;
        this.separator = separator;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        Map<String, List<Feature>> feaMap = new HashMap<>();
        if (entity.has(attrName)) {
            List<Feature> features = new ArrayList<>();
            String attr = entity.get(attrName).asText();
            String[] fields = attr.split(separator);
            for (String field : fields) {
                double value = Double.parseDouble(field);
                FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(features, update,
                        indexSpace, indexName, attrName, value);
            }
            feaMap.put(feaName, features);
        } else {
            logger.warn("{} is not present in {}", attrName, entity);
        }
        return feaMap;
    }
}
