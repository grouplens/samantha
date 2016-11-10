package org.grouplens.samantha.server.indexer;

import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.common.RedisService;

import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class RedisBasedIndexerConfig implements IndexerConfig {
    final String keyFieldsKey;
    final String hashFieldsKey;
    final String sortFieldKey;
    final String indexPrefixKey;
    final String indexStructureKey;
    final String daoConfigKey;
    final Configuration daoConfigs;
    final Injector injector;

    private RedisBasedIndexerConfig(String keyFieldsKey, String sortFieldKey, String indexPrefixKey,
                                    String hashFieldsKey, Configuration daoConfigs, Injector injector,
                                    String indexStructureKey, String daoConfigKey) {
        this.keyFieldsKey = keyFieldsKey;
        this.sortFieldKey = sortFieldKey;
        this.indexPrefixKey = indexPrefixKey;
        this.daoConfigs = daoConfigs;
        this.injector = injector;
        this.hashFieldsKey = hashFieldsKey;
        this.indexStructureKey = indexStructureKey;
        this.daoConfigKey = daoConfigKey;
    }

    public static IndexerConfig getIndexerConfig(Configuration indexerConfig,
                                          Injector injector) {
        return new RedisBasedIndexerConfig(indexerConfig.getString("keyFieldsKey"),
                indexerConfig.getString("sortFieldKey"), indexerConfig.getString("indexPrefixKey"),
                indexerConfig.getString("hashFieldsKey"),
                indexerConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get()), injector,
                indexerConfig.getString("indexStructureKey"),
                indexerConfig.getString("daoConfigKey"));
    }

    public Indexer getIndexer(RequestContext requestContext) {
        RedisService redisService = injector.instanceOf(RedisService.class);
        String indexStructure = JsonHelpers.getOptionalString(requestContext.getRequestBody(),
                indexStructureKey);
        RedisBasedIndexer.IndexStructure structure = RedisBasedIndexer.IndexStructure.HASH_SET;
        if (indexStructure != null) {
            structure = RedisBasedIndexer.IndexStructure.valueOf(indexStructure);
        }
        return new RedisBasedIndexer(redisService, this, structure);
    }
}
