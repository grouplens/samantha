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
import org.grouplens.samantha.server.retriever.ESQueryBasedRetriever;
import org.grouplens.samantha.server.retriever.ESRetrieverUtilities;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class ESBasicItemFilterExpander implements EntityExpander {
    final private ESQueryBasedRetriever retriever;
    final private List<String> itemAttrs;
    final private String defaultMatch;
    final private String elasticSearchReqKey;

    public ESBasicItemFilterExpander(ESQueryBasedRetriever retriever, List<String> itemAttrs,
                                     String defaultMatch, String elasticSearchReqKey) {
        this.retriever = retriever;
        this.itemAttrs = itemAttrs;
        this.defaultMatch = defaultMatch;
        this.elasticSearchReqKey = elasticSearchReqKey;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        String retrieverName = expanderConfig.getString("retrieverName");
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        ESQueryBasedRetriever retriever = (ESQueryBasedRetriever) configService.getRetriever(retrieverName,
                requestContext);
        return new ESBasicItemFilterExpander(retriever, expanderConfig.getStringList("itemAttrs"),
                expanderConfig.getString("defaultMatch"), expanderConfig.getString("elasticSearchReqKey"));
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                            RequestContext requestContext) {
        RetrievedResult filters = ESRetrieverUtilities.requestOrDefaultMatch(requestContext, retriever,
                defaultMatch, elasticSearchReqKey);
        return ExpanderUtilities.basicItemFilter(initialResult, filters, itemAttrs, true);
    }
}
