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
import org.grouplens.samantha.modeler.knn.KnnModelTrigger;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.Logger;
import play.inject.Injector;

import java.util.List;

public class ItemKnnRetriever extends AbstractRetriever {
    private final Retriever retriever;
    private final KnnModelTrigger trigger;

    public ItemKnnRetriever(Retriever retriever,
                            KnnModelTrigger trigger,
                            Configuration config,
                            RequestContext requestContext,
                            Injector injector) {
        super(config, requestContext, injector);
        this.retriever = retriever;
        this.trigger = trigger;
    }

    public RetrievedResult retrieve(RequestContext requestContext) {
        long start = System.currentTimeMillis();
        RetrievedResult interactions = retriever.retrieve(requestContext);
        Logger.debug("Redis retrieving time: {}", System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        List<ObjectNode> results = trigger.getTriggeredFeatures(interactions.getEntityList());
        Logger.debug("Feature trigger time: {}", System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        results = ExpanderUtilities.expand(results, postExpanders, requestContext);
        Logger.debug("Expanding time: {}", System.currentTimeMillis() - start);
        interactions.setEntityList(results);
        return interactions;
    }
}
