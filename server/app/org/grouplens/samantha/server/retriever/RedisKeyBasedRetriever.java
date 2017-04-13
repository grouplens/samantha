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
import org.grouplens.samantha.server.common.RedisService;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

public class RedisKeyBasedRetriever extends AbstractRetriever {
    private final RedisService redisService;
    private final List<String> retrieveFields;
    private final String indexPrefix;
    private final List<String> keyFields;
    private final List<EntityExpander> expanders;

    public RedisKeyBasedRetriever(RedisService redisService, List<String> retrieveFields,
                                  String indexPrefix, List<String> keyFields, Configuration config,
                                  List<EntityExpander> expanders) {
        super(config);
        this.keyFields = keyFields;
        this.redisService = redisService;
        this.retrieveFields = retrieveFields;
        this.indexPrefix = indexPrefix;
        this.expanders = expanders;
    }

    private List<ObjectNode> retrieve(JsonNode data) {
        //TODO: based on different situations in data, different set of results need to be retrieved
        List<JsonNode> results = redisService.bulkGetFromHashSet(indexPrefix, keyFields, data);
        List<ObjectNode> list = new ArrayList<>(results.size());
        for (JsonNode result : results) {
            ObjectNode entity = Json.newObject();
            IOUtilities.parseEntityFromJsonNode(retrieveFields, result, entity);
            list.add(entity);
        }
        return list;
    }

    public RetrievedResult retrieve(RequestContext requestContext) {
        JsonNode reqBody = requestContext.getRequestBody();
        List<ObjectNode> hits = retrieve(reqBody);
        hits = ExpanderUtilities.expand(hits, expanders, requestContext);
        return new RetrievedResult(hits, hits.size());
    }
}
