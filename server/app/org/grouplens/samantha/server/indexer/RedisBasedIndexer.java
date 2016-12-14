package org.grouplens.samantha.server.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.common.RedisService;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

//TODO: support remove according to keyFields or/and hashFields
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
                             String daoConfigKey, Configuration config) {
        super(config, configService, daoConfigs, daoConfigKey, injector);
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
        structure.bulkIndex(prefix, data, this, service, requestContext.getRequestBody());
    }
}
