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

package org.grouplens.samantha.server.ranker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class FieldBlendingRanker extends AbstractRanker {
    private final Object2DoubleMap<String> defaults;
    private final int offset;
    private final int pageSize;
    private final int limit;

    public FieldBlendingRanker(Object2DoubleMap<String> defaults, int offset, int limit, int pageSize,
                               RequestContext requestContext, Injector injector, Configuration config) {
        super(config, requestContext, injector);
        this.defaults = defaults;
        this.offset = offset;
        this.limit = limit;
        this.pageSize = pageSize;
    }

    public RankedResult rank(RetrievedResult retrievedResult, RequestContext requestContext) {
        List<ObjectNode> entityList = retrievedResult.getEntityList();
        entityList = ExpanderUtilities.expand(entityList, expanders, requestContext);
        int curLimit = limit;
        if (pageSize == 0 || limit > entityList.size()) {
            curLimit = entityList.size();
        }
        Object2DoubleMap<String> field2min = new Object2DoubleOpenHashMap<>(defaults.size());
        Object2DoubleMap<String> field2max = new Object2DoubleOpenHashMap<>(defaults.size());
        for (Object2DoubleMap.Entry<String> entry : defaults.object2DoubleEntrySet()) {
            String key = entry.getKey();
            for (JsonNode entity : entityList) {
                if (entity.has(key)) {
                    double curVal = entity.get(key).asDouble();
                    double curMin = field2min.getOrDefault(key, Double.MAX_VALUE);
                    double curMax = field2max.getOrDefault(key, Double.MIN_VALUE);
                    if (curMin > curVal) {
                        field2min.put(key, curVal);
                    }
                    if (curMax < curVal) {
                        field2max.put(key, curVal);
                    }
                }
            }
        }
        List<Prediction> scoredList = new ArrayList<>(entityList.size());
        for (ObjectNode entity : entityList) {
            double score = 0.0;
            for (Object2DoubleMap.Entry<String> entry : defaults.object2DoubleEntrySet()) {
                String key = entry.getKey();
                if (entity.has(key)) {
                    double min = field2min.getDouble(key);
                    double max = field2max.getDouble(key);
                    double val = (min == max) ? 0.0 : (entity.get(key).asDouble() - min) / (max - min);
                    score += (entry.getDoubleValue() * val);
                }
            }
            scoredList.add(new Prediction(entity, null, score, null));
        }
        Ordering<Prediction> ordering = RankerUtilities.scoredResultScoreOrdering();
        List<Prediction> candidates = ordering
                .greatestOf(scoredList, offset + curLimit);
        List<Prediction> recs;
        if (candidates.size() < offset) {
            recs = new ArrayList<>();
        } else {
            recs = candidates.subList(offset, candidates.size());
        }
        return new RankedResult(recs, offset, curLimit, scoredList.size(),
                postExpanders, requestContext);
    }
}
