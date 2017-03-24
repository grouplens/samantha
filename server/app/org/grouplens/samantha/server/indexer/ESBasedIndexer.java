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

package org.grouplens.samantha.server.indexer;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.action.bulk.BulkResponse;
import org.grouplens.samantha.server.common.ElasticSearchService;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

//TODO: support remove and upsert options according to uniqueKeys
public class ESBasedIndexer extends AbstractIndexer {
    private final ElasticSearchService elasticSearchService;
    private final String elasticSearchIndex;
    private final String indexTypeKey;
    private final String indexType;
    private final String uniqueFieldsKey;
    private final List<String> uniqueFields;

    public ESBasedIndexer(ElasticSearchService elasticSearchService,
                          SamanthaConfigService configService,
                          Configuration daoConfigs,
                          String elasticSearchIndex,
                          String indexTypeKey,
                          String indexType,
                          String uniqueFieldsKey,
                          List<String> uniqueFields,
                          Injector injector, String daoConfigKey,
                          Configuration config) {
        super(config, configService, daoConfigs, daoConfigKey, injector);
        this.elasticSearchService = elasticSearchService;
        this.uniqueFieldsKey = uniqueFieldsKey;
        this.indexTypeKey = indexTypeKey;
        this.elasticSearchIndex = elasticSearchIndex;
        this.indexType = indexType;
        this.uniqueFields = uniqueFields;
    }

    private BulkResponse bulkIndex(String indexType, JsonNode data) {
        //TODO: first check whether existing based on config.uniqueFieldsKey if upsert
        return elasticSearchService.bulkIndex(elasticSearchIndex, indexType, data);
    }

    public void index(JsonNode data, RequestContext requestContext) {
        String indexType = JsonHelpers.getOptionalString(requestContext.getRequestBody(),
                indexTypeKey, this.indexType);
        BulkResponse resp = bulkIndex(indexType, data);
        if (resp.hasFailures()) {
            throw new BadRequestException(resp.buildFailureMessage());
        }
    }

    public ObjectNode getIndexedDataDAOConfig(RequestContext requestContext) {
        throw new BadRequestException("Reading data from this indexer is not supported.");
    }
}
