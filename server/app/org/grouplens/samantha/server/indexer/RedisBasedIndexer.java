package org.grouplens.samantha.server.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.common.RedisService;
import org.grouplens.samantha.server.dao.EntityDAOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Logger;

import java.util.List;

//TODO: support remove according to keyFields or/and hashFields
public class RedisBasedIndexer implements Indexer {
    private final RedisBasedIndexerConfig config;
    private final RedisService service;
    private final IndexStructure structure;

    private interface IndexMethods {
        void bulkIndex(String prefix, JsonNode data, RedisBasedIndexerConfig config,
                       RedisService service, JsonNode reqBody);
    }

    enum IndexStructure implements IndexMethods {
        HASH_SET("HashSet") {
            public void bulkIndex(String prefix, JsonNode data, RedisBasedIndexerConfig config,
                                  RedisService service, JsonNode reqBody) {
                List<String> hashFields = JsonHelpers.getRequiredStringList(reqBody, config.hashFieldsKey);
                List<String> keyFields = JsonHelpers.getRequiredStringList(reqBody, config.keyFieldsKey);
                service.bulkIndexIntoHashSet(prefix, keyFields, hashFields, data);
            }
        },
        SORTED_SET("SortedSet") {
            public void bulkIndex(String prefix, JsonNode data, RedisBasedIndexerConfig config,
                                  RedisService service, JsonNode reqBody) {
                List<String> keyFields = JsonHelpers.getRequiredStringList(reqBody, config.keyFieldsKey);
                String sortField = JsonHelpers.getRequiredString(reqBody, config.sortFieldKey);
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

    public RedisBasedIndexer(RedisService service, RedisBasedIndexerConfig config, IndexStructure structure) {
        this.config = config;
        this.service = service;
        this.structure = structure;
    }

    public void index(RequestContext requestContext) {
        JsonNode reqBody = requestContext.getRequestBody();
        String prefix = JsonHelpers.getRequiredString(reqBody, config.indexPrefixKey);
        EntityDAO entityDAO = EntityDAOUtilities.getEntityDAO(config.daoConfigs, requestContext,
                reqBody.get(config.daoConfigKey), config.injector);
        int cnt = 0;
        while (entityDAO.hasNextEntity()) {
            JsonNode documents = entityDAO.getNextEntity();
            structure.bulkIndex(prefix, documents, config, service, reqBody);
            cnt++;
            if (cnt % 10000 == 0) {
                Logger.info("Indexed {} entities.", cnt);
            }
        }
    }

    public void index(String type, JsonNode data, RequestContext requestContext) {
        structure.bulkIndex(type, data, config, service, requestContext.getRequestBody());
    }
}
