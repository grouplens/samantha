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
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.predictor.Prediction;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class EntityFieldRanker extends AbstractRanker {
    private final int offset;
    private final int limit;
    private final int pageSize;
    private final boolean whetherOrder;
    private final String orderField;
    private final boolean ascending;

    public EntityFieldRanker(Configuration config, RequestContext requestContext, Injector injector,
                             int offset, int limit, int pageSize,
                             boolean whetherOrder, String orderField, boolean ascending) {
        super(config, requestContext, injector);
        this.offset = offset;
        this.limit = limit;
        this.pageSize = pageSize;
        this.whetherOrder = whetherOrder;
        this.orderField = orderField;
        this.ascending = ascending;
    }

    public RankedResult rank(RetrievedResult retrievedResult,
                             RequestContext requestContext) {
        List<ObjectNode> entityList = retrievedResult.getEntityList();
        entityList = ExpanderUtilities.expand(entityList, expanders, requestContext);
        int curLimit = limit;
        if (pageSize == 0 || limit > entityList.size()) {
            curLimit = entityList.size();
        }
        List<Prediction> scoredList = new ArrayList<>(entityList.size());
        for (ObjectNode entity : entityList) {
            scoredList.add(new Prediction(entity, null,
                    entity.get(orderField).asDouble(), null));
        }
        List<Prediction> candidates;
        if (whetherOrder) {
            Ordering<Prediction> ordering = RankerUtilities.scoredResultFieldOrdering(orderField);
            if (ascending) {
                candidates = ordering.leastOf(scoredList, offset + curLimit);
            } else {
                candidates = ordering.greatestOf(scoredList, offset + curLimit);
            }
        } else {
            candidates = scoredList;
        }
        List<Prediction> recs;
        if (offset >= candidates.size()) {
            recs = new ArrayList<>();
        } else {
            recs = candidates.subList(offset, curLimit);
        }
        return new RankedResult(recs, offset, curLimit, retrievedResult.getMaxHits());
    }
}
