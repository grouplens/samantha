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

package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import org.grouplens.samantha.server.retriever.Retriever;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

public class RetrieverBasedExpander implements EntityExpander {
    final private Retriever retriever;

    public RetrieverBasedExpander(Retriever retriever) {
        this.retriever = retriever;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        String retrieverName = expanderConfig.getString("retrieverName");
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        Retriever retriever = configService.getRetriever(retrieverName, requestContext);
        return new RetrieverBasedExpander(retriever);
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        List<ObjectNode> expanded = new ArrayList<>();
        for (ObjectNode entity : initialResult) {
            ObjectNode reqBody = Json.newObject();
            IOUtilities.parseEntityFromJsonNode(requestContext.getRequestBody(), reqBody);
            IOUtilities.parseEntityFromJsonNode(entity, reqBody);
            RequestContext pseudoReq = new RequestContext(reqBody, requestContext.getEngineName());
            RetrievedResult retrievedResult = retriever.retrieve(pseudoReq);
            for (ObjectNode one : retrievedResult.getEntityList()) {
                IOUtilities.parseEntityFromJsonNode(entity, one);
            }
            expanded.addAll(retrievedResult.getEntityList());
        }
        return expanded;
    }
}
