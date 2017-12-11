package org.grouplens.samantha.server.indexer;

import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class NullIndexerConfig implements IndexerConfig {
    private final Injector injector;
    private final Configuration config;
    private final Configuration daoConfigs;
    private final String daoConfigKey;

    private NullIndexerConfig(Injector injector, Configuration config,
                                  Configuration daoConfigs, String daoConfigKey) {
        this.injector = injector;
        this.config = config;
        this.daoConfigKey = daoConfigKey;
        this.daoConfigs = daoConfigs;
    }

    public static IndexerConfig getIndexerConfig(Configuration indexerConfig,
                                                 Injector injector) {
        return new NullIndexerConfig(injector, indexerConfig,
                indexerConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get()),
                indexerConfig.getString("daoConfigKey"));
    }

    public Indexer getIndexer(RequestContext requestContext) {
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        return new NullIndexer(config, configService, injector, daoConfigKey, daoConfigs);
    }
}
