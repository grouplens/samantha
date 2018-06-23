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
import org.grouplens.samantha.modeler.model.IndexSpace;
import org.grouplens.samantha.modeler.tensorflow.TensorFlowModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultipleSeparatedStringExtractor implements FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private static Logger logger = LoggerFactory.getLogger(MultipleSeparatedStringExtractor.class);
    private final List<String> indexNames;
    private final List<String> attrNames;
    private final List<String> keyPrefixes;
    private final List<String> feaNames;
    private final String separator;
    private final boolean normalize;
    private final String fillIn;

    public MultipleSeparatedStringExtractor(
            List<String> indexNames,
                                            List<String> attrNames,
                                            List<String> keyPrefixes,
                                            List<String> feaNames,
                                            String separator,
                                            boolean normalize,
                                            String fillIn) {
        this.indexNames = indexNames;
        this.attrNames = attrNames;
        if (keyPrefixes != null) {
            this.keyPrefixes = keyPrefixes;
        } else {
            this.keyPrefixes = attrNames;
        }
        this.feaNames = feaNames;
        this.separator = separator;
        this.normalize = normalize;
        this.fillIn = fillIn;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        Map<String, List<Feature>> feaMap = new HashMap<>();
        if (entity.has(attrNames.get(0)) || fillIn != null) {
            String attr = fillIn;
            if (entity.has(attrNames.get(0))) {
                attr = entity.get(attrNames.get(0)).asText();
            }
            if (!"".equals(attr) || "".equals(fillIn)) {
                for (int k=0; k<attrNames.size(); k++) {
                    List<Feature> features = new ArrayList<>();
                    attr = entity.get(attrNames.get(k)).asText();
                    String[] fields = attr.split(separator, -1);
                    double val = 1.0;
                    if (fields.length > 0 && normalize) {
                        val = 1.0 / Math.sqrt(fields.length);
                    }
                    for (int i = 0; i < fields.length; i++) {
                        String field = fields[i];
                        String key;
                        if ("".equals(field)) {
                            key = TensorFlowModel.OOV;
                        } else {
                            key = FeatureExtractorUtilities.composeKey(keyPrefixes.get(k), field);
                        }
                        FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(features, update,
                                indexSpace, indexNames.get(k), key, val);
                    }
                    feaMap.put(feaNames.get(k), features);
                }
            } else {
                for (int k=0; k<feaNames.size(); k++) {
                    feaMap.put(feaNames.get(k), new ArrayList<>());
                }
            }
        }
        if (!entity.has(attrNames.get(0))){
            logger.warn("{} is not present in {}", attrNames.get(0), entity);
        }
        return feaMap;
    }
}
