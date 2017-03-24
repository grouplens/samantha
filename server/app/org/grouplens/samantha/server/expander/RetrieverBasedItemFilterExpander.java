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

package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.retriever.Retriever;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class RetrieverBasedItemFilterExpander implements EntityExpander {
    final private Retriever retriever;
    final private List<String> itemAttrs;
    final private boolean exclude;

    public RetrieverBasedItemFilterExpander(Retriever retriever, List<String> itemAttrs,
                                            boolean exclude) {
        this.retriever = retriever;
        this.itemAttrs = itemAttrs;
        this.exclude = exclude;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        String retrieverName = expanderConfig.getString("retrieverName");
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        Retriever retriever = configService.getRetriever(retrieverName, requestContext);
        boolean exclude = true;
        if (expanderConfig.asMap().containsKey("exclude")) {
            exclude = expanderConfig.getBoolean("exclude");
        }
        return new RetrieverBasedItemFilterExpander(retriever, expanderConfig.getStringList("itemAttrs"),
                exclude);
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        RetrievedResult filters = retriever.retrieve(requestContext);
        return ExpanderUtilities.basicItemFilter(initialResult, filters, itemAttrs, exclude);
    }
}
