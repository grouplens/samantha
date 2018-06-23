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

package org.grouplens.samantha.server.featurizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.featurizer.Feature;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.Predictor;
import org.grouplens.samantha.modeler.model.IndexSpace;
import play.libs.Json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Use of this class is discouraged. Instead, use {@link org.grouplens.samantha.server.expander.PredictorBasedExpander
 * PredictorBasedExpander} together with a {@link org.grouplens.samantha.modeler.featurizer.IdentityExtractor
 * IdentityExtractor}.
 */
public class PredictorBasedExtractor implements FeatureExtractor {
    transient private final Predictor predictor;
    transient private final RequestContext requestContext;
    private final String feaName;
    private final String indexName;

    public PredictorBasedExtractor(Predictor predictor, RequestContext requestContext,
                                   String feaName, String indexName) {
        this.feaName = feaName;
        this.indexName = indexName;
        this.predictor = predictor;
        this.requestContext = requestContext;
    }

    public Map<String, List<Feature>> extract(JsonNode json, boolean update,
                                              IndexSpace indexSpace) {
        List<ObjectNode> entityList = new ArrayList<>();
        ObjectNode entity = Json.newObject();
        IOUtilities.parseEntityFromJsonNode(json, entity);
        entityList.add(entity);
        List<Prediction> predictions = predictor.predict(entityList, requestContext);
        double val = predictions.get(0).getScore();
        List<Feature> feaList = new ArrayList<>(1);
        FeatureExtractorUtilities.getOrSetIndexSpaceToFeaturize(feaList, update,
                indexSpace, indexName, feaName, val);
        Map<String, List<Feature>> feaMap = new HashMap<>();
        feaMap.put(feaName, feaList);
        return feaMap;
    }
}
