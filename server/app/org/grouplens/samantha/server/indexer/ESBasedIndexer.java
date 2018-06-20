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

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.server.common.DataOperation;
import org.grouplens.samantha.server.common.ElasticSearchService;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.util.*;

public class ESBasedIndexer extends AbstractIndexer {
    private final ElasticSearchService elasticSearchService;
    private final String elasticSearchIndex;
    private final String indexTypeKey;
    private final String indexType;
    private final List<String> uniqueFields;
    private final List<String> dataFields;

    public ESBasedIndexer(ElasticSearchService elasticSearchService,
                          SamanthaConfigService configService,
                          Configuration daoConfigs,
                          String elasticSearchIndex,
                          String indexTypeKey,
                          String indexType,
                          List<String> uniqueFields,
                          Injector injector, String daoConfigKey,
                          Configuration config, int batchSize, RequestContext requestContext) {
        super(config, configService, daoConfigs, daoConfigKey, batchSize, requestContext, injector);
        this.elasticSearchService = elasticSearchService;
        this.indexTypeKey = indexTypeKey;
        this.elasticSearchIndex = elasticSearchIndex;
        this.indexType = indexType;
        this.uniqueFields = uniqueFields;
        this.dataFields = new ArrayList<>();
        for (String field : this.uniqueFields) {
            this.dataFields.add(field.replace(".raw", ""));
        }
    }

    private void bulkIndex(String indexType, JsonNode data) {
        if (data.size() == 0) {
            return;
        }
        if (uniqueFields.size() > 0) {
            Set<String> keys = new HashSet<>();
            ArrayNode uniqued = Json.newArray();
            for (JsonNode point : data) {
                String key = FeatureExtractorUtilities.composeConcatenatedKey(point, dataFields);
                if (!keys.contains(key)) {
                    keys.add(key);
                    uniqued.add(point);
                }
            }
            data = uniqued;
        }
        BulkResponse resp = elasticSearchService.bulkIndex(elasticSearchIndex, indexType, data);
        if (resp.hasFailures()) {
            throw new BadRequestException(resp.buildFailureMessage());
        }
    }

    private void bulkDelete(String indexType, JsonNode data) {
        SearchHits hits = elasticSearchService
                .searchHitsByKeys(elasticSearchIndex, indexType, uniqueFields, uniqueFields, data);
        List<String> ids = new ArrayList<>();
        for (SearchHit hit : hits.getHits()) {
            if (hit != null) {
                ids.add(hit.getId());
            }
        }
        if (ids.size() == 0) {
            return;
        }
        BulkResponse resp = elasticSearchService.bulkDelete(elasticSearchIndex, indexType, ids);
        if (resp.hasFailures()) {
            throw new BadRequestException(resp.buildFailureMessage());
        }
    }

    public void index(JsonNode data, RequestContext requestContext) {
        JsonNode reqBody = requestContext.getRequestBody();
        String indexType = JsonHelpers.getOptionalString(reqBody,
                indexTypeKey, this.indexType);
        String operation = JsonHelpers.getOptionalString(reqBody, ConfigKey.DATA_OPERATION.get(),
                DataOperation.INSERT.get());
        if (operation.equals(DataOperation.DELETE.get()) || operation.equals(DataOperation.UPSERT.get())) {
            bulkDelete(indexType, data);
        }
        if (operation.equals(DataOperation.INSERT.get()) || operation.equals(DataOperation.UPSERT.get())) {
            bulkIndex(indexType, data);
        }
    }
}
