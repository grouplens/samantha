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

package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FeatureSupportRetriever extends AbstractRetriever {
    final private SVDFeature model;
    final private List<String> itemAttrs;
    final private String supportAttr;
    final private Integer maxHits;
    final private List<EntityExpander> expanders;
    volatile private List<ObjectNode> cached;

    public FeatureSupportRetriever(SVDFeature model, List<String> itemAttrs, String supportAttr,
                                   Integer maxHits, List<EntityExpander> expanders, Configuration config) {
        super(config);
        this.model = model;
        this.itemAttrs = itemAttrs;
        this.supportAttr = supportAttr;
        this.maxHits = maxHits;
        this.expanders = expanders;
    }

    public RetrievedResult retrieve(RequestContext requestContext) {
        List<ObjectNode> entities;
        if (cached == null) {
            Object2DoubleMap<String> fea2sup = model.getFactorFeatures(10);
            List<ObjectNode> all = new ArrayList<>(fea2sup.size());
            for (Object2DoubleMap.Entry<String> entry : fea2sup.object2DoubleEntrySet()) {
                ObjectNode one = Json.newObject();
                one.put(supportAttr, entry.getDoubleValue());
                Map<String, String> keys = FeatureExtractorUtilities.decomposeKey(entry.getKey());
                boolean include = true;
                for (String attr : itemAttrs) {
                    if (!keys.containsKey(attr)) {
                        include = false;
                    }
                }
                if (include) {
                    for (String attr : itemAttrs) {
                        one.put(attr, keys.get(attr));
                    }
                    all.add(one);
                }
            }
            Ordering<ObjectNode> ordering = RetrieverUtilities.jsonFieldOrdering(supportAttr);
            int limit = all.size();
            if (maxHits != null && maxHits < limit) {
                limit = maxHits;
            }
            entities = ordering.greatestOf(all, limit);
            cached = entities;
        } else {
            entities = cached;
        }
        entities = ExpanderUtilities.expand(entities, expanders, requestContext);
        return new RetrievedResult(entities, entities.size());
    }
}
