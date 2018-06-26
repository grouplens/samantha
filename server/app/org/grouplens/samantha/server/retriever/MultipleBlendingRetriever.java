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

import com.fasterxml.jackson.databind.node.ObjectNode;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.Logger;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class MultipleBlendingRetriever extends AbstractRetriever {
    private final List<Retriever> retrievers;
    private final Integer maxHits;
    private final List<String> itemAttrs;

    public MultipleBlendingRetriever(List<Retriever> retrievers, List<String> itemAttrs, Integer maxHits,
                                     Configuration config, RequestContext requestContext, Injector injector) {
        super(config, requestContext, injector);
        this.maxHits = maxHits;
        this.retrievers = retrievers;
        this.itemAttrs = itemAttrs;
    }

    public RetrievedResult retrieve(RequestContext requestContext) {
        ObjectSet<String> items = new ObjectOpenHashSet<>();
        List<ObjectNode> entities = new ArrayList<>(maxHits);
        for (Retriever retriever : retrievers) {
            long start = System.currentTimeMillis();
            RetrievedResult results = retriever.retrieve(requestContext);
            Logger.debug("{} time: {}", retriever, System.currentTimeMillis() - start);
            List<ObjectNode> initial = results.getEntityList();
            initial = ExpanderUtilities.expand(initial, expanders, requestContext);
            for (ObjectNode entity : initial) {
                String item = FeatureExtractorUtilities.composeConcatenatedKey(entity, itemAttrs);
                if (!items.contains(item)) {
                    items.add(item);
                    entities.add(entity);
                    if (maxHits != null && entities.size() >= maxHits) {
                        return new RetrievedResult(entities, maxHits);
                    }
                }
            }
        }
        return new RetrievedResult(entities, entities.size());
    }
}
