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

package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.modeler.knn.KnnModelTrigger;
import org.grouplens.samantha.modeler.tree.SortingUtilities;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

public class UserKnnRetriever extends AbstractRetriever {
    private final Retriever retriever;
    private final KnnModelTrigger trigger;
    private final String weightAttr;
    private final String scoreAttr;
    private final List<String> itemAttrs;
    private final List<String> userAttrs;

    public UserKnnRetriever(String weightAttr,
                            String scoreAttr,
                            List<String> userAttrs,
                            List<String> itemAttrs,
                            Retriever retriever,
                            KnnModelTrigger trigger,
                            Configuration config,
                            RequestContext requestContext,
                            Injector injector) {
        super(config, requestContext, injector);
        this.weightAttr = weightAttr;
        this.scoreAttr = scoreAttr;
        this.itemAttrs = itemAttrs;
        this.userAttrs = userAttrs;
        this.retriever = retriever;
        this.trigger = trigger;
    }

    public RetrievedResult retrieve(RequestContext requestContext) {
        String engineName = requestContext.getEngineName();
        JsonNode reqBody = requestContext.getRequestBody();
        List<ObjectNode> initial = new ArrayList<>();
        ObjectNode one = Json.newObject();
        IOUtilities.parseEntityFromJsonNode(reqBody, one);
        initial.add(one);
        List<ObjectNode> features = trigger.getTriggeredFeatures(initial);
        ArrayNode arrFeas = Json.newArray();
        Object2DoubleMap<String> items = new Object2DoubleOpenHashMap<>();
        Object2DoubleMap<String> feature2score = new Object2DoubleOpenHashMap<>();
        for (ObjectNode feature : features) {
            arrFeas.add(feature);
            String key = FeatureExtractorUtilities.composeConcatenatedKey(feature, userAttrs);
            feature2score.put(key, feature.get(scoreAttr).asDouble());
        }
        RequestContext pseudoReq = new RequestContext(arrFeas, engineName);
        RetrievedResult retrieved = retriever.retrieve(pseudoReq);
        List<ObjectNode> results = new ArrayList<>();
        for (ObjectNode entity : retrieved.getEntityList()) {
            double weight = 1.0;
            if (entity.has(weightAttr)) {
                weight = entity.get(weightAttr).asDouble();
            }
            if (weight >= 0.5) {
                String feature = FeatureExtractorUtilities.composeConcatenatedKey(entity, userAttrs);
                double score = feature2score.getDouble(feature);
                String key = FeatureExtractorUtilities.composeConcatenatedKey(entity, itemAttrs);
                if (items.containsKey(key)) {
                    items.put(key, items.getDouble(key) + weight * score);
                } else {
                    items.put(key, weight * score);
                    results.add(entity);
                }
            }
        }
        for (ObjectNode result : results) {
            String key = FeatureExtractorUtilities.composeConcatenatedKey(result, itemAttrs);
            result.put(scoreAttr, items.getDouble(key));
        }
        results = ExpanderUtilities.expand(results, postExpanders, requestContext);
        results.sort(SortingUtilities.jsonFieldReverseComparator(scoreAttr));
        return new RetrievedResult(results, results.size());
    }
}
