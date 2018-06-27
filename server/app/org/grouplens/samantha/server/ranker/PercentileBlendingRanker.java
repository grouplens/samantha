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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.grouplens.samantha.modeler.tree.SortingUtilities;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class PercentileBlendingRanker extends AbstractRanker {
    private final Object2DoubleMap<String> defaults;
    private final int offset;
    private final int pageSize;
    private final int limit;

    public PercentileBlendingRanker(Object2DoubleMap<String> defaults, int offset, int limit, int pageSize,
                                    Configuration config, RequestContext requestContext, Injector injector) {
        super(config, requestContext, injector);
        this.defaults = defaults;
        this.offset = offset;
        this.limit = limit;
        this.pageSize = pageSize;
    }

    public RankedResult rank(RetrievedResult retrievedResult, RequestContext requestContext) {
        List<ObjectNode> entityList = retrievedResult.getEntityList();
        entityList = ExpanderUtilities.expand(entityList, expanders, requestContext);
        int listSize = entityList.size();
        if (listSize > 0) {
            for (Object2DoubleMap.Entry<String> entry : defaults.object2DoubleEntrySet()) {
                String key = entry.getKey();
                entityList.sort(SortingUtilities.jsonFieldComparator(key));
                for (int i = 0; i < entityList.size(); i++) {
                    entityList.get(i).put(key + "Percentile", (double) i / listSize);
                }
            }
        }
        int curLimit = limit;
        if (pageSize == 0 || limit > listSize) {
            curLimit = entityList.size();
        }
        List<Prediction> scoredList = new ArrayList<>(entityList.size());
        for (ObjectNode entity : entityList) {
            double score = 0.0;
            for (Object2DoubleMap.Entry<String> entry : defaults.object2DoubleEntrySet()) {
                String key = entry.getKey();
                score += (entry.getDoubleValue() * entity.get(key + "Percentile").asDouble());
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
