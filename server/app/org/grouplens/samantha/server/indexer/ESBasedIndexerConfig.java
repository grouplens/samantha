package org.grouplens.samantha.server.indexer;

import org.grouplens.samantha.server.common.ElasticSearchService;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import javax.inject.Inject;
import java.util.List;

public class ESBasedIndexerConfig implements IndexerConfig {
    private final Injector injector;
    private final Configuration daoConfigs;
    private final String elasticSearchIndex;
    private final String indexTypeKey;
    private final String indexType;
    private final String uniqueFieldsKey;
    private final List<String> uniqueFields;
    private final String daoConfigKey;
    private final Configuration config;

    @Inject
    private ESBasedIndexerConfig(Configuration daoConfigs,
                                 String elasticSearchIndex,
                                 String indexTypeKey,
                                 String indexType,
                                 String uniqueFieldsKey,
                                 List<String> uniqueFields,
                                 Injector injector, String daoConfigKey,
                                 Configuration config) {
        this.config = config;
        this.injector = injector;
        this.uniqueFieldsKey = uniqueFieldsKey;
        this.daoConfigs = daoConfigs;
        this.indexTypeKey = indexTypeKey;
        this.elasticSearchIndex = elasticSearchIndex;
        this.daoConfigKey = daoConfigKey;
        this.indexType = indexType;
        this.uniqueFields = uniqueFields;
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
        return new ESBasedIndexerConfig(indexerConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get()),
                elasticSearchIndex, indexerConfig.getString("indexTypeKey"),
                indexerConfig.getString("indexType"),
                indexerConfig.getString("uniqueFieldsKey"),
                indexerConfig.getStringList("uniqueFields"),
                injector, indexerConfig.getString("daoConfigKey"),
                indexerConfig);
    }

    public Indexer getIndexer(RequestContext requestContext) {
        ElasticSearchService elasticSearchService = injector
                .instanceOf(ElasticSearchService.class);
        SamanthaConfigService configService = injector
                .instanceOf(SamanthaConfigService.class);
        return new ESBasedIndexer(elasticSearchService, configService, daoConfigs, elasticSearchIndex,
                indexTypeKey, indexType, uniqueFieldsKey, uniqueFields, injector, daoConfigKey, config);
    }
}
