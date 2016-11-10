package org.grouplens.samantha.server.indexer;

import org.grouplens.samantha.server.common.ElasticSearchService;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import javax.inject.Inject;

public class ESBasedIndexerConfig implements IndexerConfig {
    final Injector injector;
    final Configuration elasticSearchSetting;
    final Configuration elasticSearchMapping;
    final Configuration daoConfigs;
    final String elasticSearchIndex;
    final String indexTypeKey;
    final String uniqueFieldsKey;
    final String daoConfigKey;

    @Inject
    private ESBasedIndexerConfig(Configuration elasticSearchMapping,
                                 Configuration elasticSearchSetting,
                                 Configuration daoConfigs,
                                 String elasticSearchIndex,
                                 String indexTypeKey,
                                 String uniqueFieldsKey,
                                 Injector injector, String daoConfigKey) {
        this.injector = injector;
        this.uniqueFieldsKey = uniqueFieldsKey;
        this.daoConfigs = daoConfigs;
        this.indexTypeKey = indexTypeKey;
        this.elasticSearchSetting = elasticSearchSetting;
        this.elasticSearchMapping = elasticSearchMapping;
        this.elasticSearchIndex = elasticSearchIndex;
        this.daoConfigKey = daoConfigKey;
    }

    static public IndexerConfig getIndexerConfig(Configuration indexerConfig,
                                                 Injector injector) {
        ElasticSearchService elasticSearchService = injector
                .instanceOf(ElasticSearchService.class);
        String elasticSearchIndex = indexerConfig.getString("elasticSearchIndex");
        Configuration settingConfig = indexerConfig.getConfig("elasticSearchSetting");
        if (!elasticSearchService.existsIndex(elasticSearchIndex).isExists()) {
            elasticSearchService.createIndex(elasticSearchIndex, settingConfig);
        }
        Configuration mappingConfig = indexerConfig.getConfig("elasticSearchMapping");
        for (String typeName : mappingConfig.subKeys()) {
            if (!elasticSearchService.existsType(elasticSearchIndex, typeName)
                    .isExists()) {
                elasticSearchService.putMapping(elasticSearchIndex, typeName,
                        mappingConfig.getConfig(typeName));
            }
        }
        return new ESBasedIndexerConfig(mappingConfig,
                settingConfig, indexerConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get()),
                elasticSearchIndex, indexerConfig.getString("indexTypeKey"),
                indexerConfig.getString("uniqueFieldsKey"),
                injector, indexerConfig.getString("daoConfigKey"));
    }

    public Indexer getIndexer(RequestContext requestContext) {
        ElasticSearchService elasticSearchService = injector
                .instanceOf(ElasticSearchService.class);
        return new ESBasedIndexer(elasticSearchService, this);
    }
}
