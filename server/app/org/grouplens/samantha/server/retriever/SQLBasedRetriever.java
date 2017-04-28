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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;

import java.util.List;

//TODO: implementation
public class SQLBasedRetriever extends AbstractRetriever {
    private final List<EntityExpander> expanders;
    private final int limit;
    private final int offset;
    private final String setScrollKey;
    private final String selectSqlKey;
    private final List<String> defMatchFields;
    private final List<String> defLessFields;
    private final List<String> defGreaterFields;

    public SQLBasedRetriever(Configuration config, List<EntityExpander> expanders, String setScrollKey,
                             int limit, int offset, String selectSqlKey, List<String> defMatchFields,
                             List<String> defGreaterFields, List<String> defLessFields) {
        super(config);
        this.expanders = expanders;
        this.offset = offset;
        this.limit = limit;
        this.setScrollKey = setScrollKey;
        this.selectSqlKey = selectSqlKey;
        this.defMatchFields = defMatchFields;
        this.defGreaterFields = defGreaterFields;
        this.defLessFields = defLessFields;
    }

    private List<ObjectNode> retrieve(JsonNode reqBody) {
        String sql;
        if (reqBody.has(selectSqlKey)) {
            sql = JsonHelpers.getRequiredString(reqBody, selectSqlKey);
        } else if (defMatchFields.size() > 0 || defGreaterFields.size() > 0 || defLessFields.size() > 0) {

        } else {
        }
        boolean setScroll = JsonHelpers.getOptionalBoolean(reqBody, setScrollKey, false);
        return null;
    }

    public RetrievedResult retrieve(RequestContext requestContext) {
        JsonNode reqBody = requestContext.getRequestBody();
        List<ObjectNode> hits = retrieve(reqBody);
        hits = ExpanderUtilities.expand(hits, expanders, requestContext);
        return new RetrievedResult(hits, hits.size());
    }
}
