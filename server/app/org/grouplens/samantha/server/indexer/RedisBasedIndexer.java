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

package org.grouplens.samantha.server.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.server.common.DataOperation;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.common.RedisService;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class RedisBasedIndexer extends AbstractIndexer {
    private final RedisService service;
    private final IndexStructure structure;
    private final String keyFieldsKey;
    private final List<String> keyFields;
    private final String hashFieldsKey;
    private final List<String> hashFields;
    private final String sortFieldKey;
    private final String sortField;
    private final String indexPrefixKey;
    private final String indexPrefix;

    private interface IndexMethods {
        void bulkIndex(String prefix, JsonNode data, RedisBasedIndexer indexer,
                       RedisService service, JsonNode reqBody);
    }

    enum IndexStructure implements IndexMethods {
        HASH_SET("HashSet") {
            public void bulkIndex(String prefix, JsonNode data, RedisBasedIndexer indexer,
                                  RedisService service, JsonNode reqBody) {
                List<String> hashFields = JsonHelpers.getOptionalStringList(reqBody, indexer.hashFieldsKey,
                        indexer.hashFields);
                List<String> keyFields = JsonHelpers.getOptionalStringList(reqBody, indexer.keyFieldsKey,
                        indexer.keyFields);
                service.bulkIndexIntoHashSet(prefix, keyFields, hashFields, data);
            }
        },
        SORTED_SET("SortedSet") {
            public void bulkIndex(String prefix, JsonNode data, RedisBasedIndexer indexer,
                                  RedisService service, JsonNode reqBody) {
                List<String> keyFields = JsonHelpers.getOptionalStringList(reqBody, indexer.keyFieldsKey,
                        indexer.keyFields);
                String sortField = JsonHelpers.getOptionalString(reqBody, indexer.sortFieldKey,
                        indexer.sortField);
                service.bulkIndexIntoSortedSet(prefix, keyFields, sortField, data);
            }
        };

        private final String key;

        IndexStructure(String key) {
            this.key = key;
        }

        public String get() {
            return this.key;
        }
    }

    public RedisBasedIndexer(RedisService service, IndexStructure structure,
                             SamanthaConfigService configService,
                             String keyFieldsKey, String sortFieldKey, String indexPrefixKey,
                             String hashFieldsKey, Configuration daoConfigs, Injector injector,
                             List<String> keyFields, List<String> hashFields,
                             String sortField, String indexPrefix,
                             String daoConfigKey, Configuration config,
                             int batchSize, RequestContext requestContext) {
        super(config, configService, daoConfigs, daoConfigKey, batchSize, requestContext, injector);
        this.service = service;
        this.structure = structure;
        this.keyFieldsKey = keyFieldsKey;
        this.sortFieldKey = sortFieldKey;
        this.indexPrefixKey = indexPrefixKey;
        this.hashFieldsKey = hashFieldsKey;
        this.keyFields = keyFields;
        this.hashFields = hashFields;
        this.sortField = sortField;
        this.indexPrefix = indexPrefix;
    }

    public void index(JsonNode data, RequestContext requestContext) {
        JsonNode reqBody = requestContext.getRequestBody();
        String prefix = JsonHelpers.getOptionalString(reqBody, indexPrefixKey, indexPrefix);
        String operation = JsonHelpers.getOptionalString(reqBody, ConfigKey.DATA_OPERATION.get(),
                DataOperation.INSERT.get());
        if (operation.equals(DataOperation.INSERT.get()) ||
                operation.equals(DataOperation.UPSERT.get())) {
            structure.bulkIndex(prefix, data, this, service, reqBody);
        } else if (operation.equals(DataOperation.DELETE.get())) {
            List<String> keyFields = JsonHelpers.getOptionalStringList(reqBody, keyFieldsKey, this.keyFields);
            service.bulkDelWithData(prefix, keyFields, data);
        }
    }
}
