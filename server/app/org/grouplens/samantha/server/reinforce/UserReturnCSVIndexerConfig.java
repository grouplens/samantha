package org.grouplens.samantha.server.reinforce;

import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.indexer.*;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

//TODO: this is just an experimental indexer config
public class UserReturnCSVIndexerConfig implements IndexerConfig {
    private final String indexerName;
    private final String timestampField;
    private final List<String> dataFields;
    private final String daoNameKey;
    private final String daoName;
    private final String filesKey;
    private final String separatorKey;
    private final String rewardKey;
    private final String userIdKey;
    private final String sessionIdKey;
    private final String filePath;
    private final String filePathKey;
    private final Configuration config;
    private final Configuration daoConfigs;
    private final String daoConfigKey;
    private final Injector injector;

    private UserReturnCSVIndexerConfig(Configuration config, Injector injector, Configuration daoConfigs,
                                       String daoConfigKey, String timestampField, List<String> dataFields,
                                       String daoNameKey, String daoName, String filesKey, String filePathKey,
                                       String separatorKey, String indexerName, String rewardKey,
                                       String userIdKey, String sessionIdKey, String filePath) {
        this.rewardKey = rewardKey;
        this.userIdKey = userIdKey;
        this.filePathKey = filePathKey;
        this.sessionIdKey = sessionIdKey;
        this.filePath = filePath;
        this.timestampField = timestampField;
        this.dataFields = dataFields;
        this.daoName = daoName;
        this.daoNameKey = daoNameKey;
        this.filesKey = filesKey;
        this.separatorKey = separatorKey;
        this.indexerName = indexerName;
        this.config = config;
        this.daoConfigs = daoConfigs;
        this.daoConfigKey = daoConfigKey;
        this.injector = injector;
    }

    public static IndexerConfig getIndexerConfig(Configuration indexerConfig,
                                                 Injector injector) {
        return new UserReturnCSVIndexerConfig(indexerConfig, injector,
                indexerConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get()),
                indexerConfig.getString("daoConfigKey"),
                indexerConfig.getString("timestampField"),
                indexerConfig.getStringList("dataFields"),
                indexerConfig.getString("daoNameKey"),
                indexerConfig.getString("daoName"),
                indexerConfig.getString("filesKey"),
                indexerConfig.getString("filePathKey"),
                indexerConfig.getString("separatorKey"),
                indexerConfig.getString("indexerName"),
                indexerConfig.getString("rewardKey"),
                indexerConfig.getString("userIdKey"),
                indexerConfig.getString("sessionIdKey"),
                indexerConfig.getString("filePath"));
    }

    public Indexer getIndexer(RequestContext requestContext) {
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        CSVFileIndexer indexer = (CSVFileIndexer) configService.getIndexer(indexerName, requestContext);
        return new UserReturnCSVIndexer(configService, config, injector, daoConfigs, daoConfigKey,
                filePathKey, timestampField, dataFields, daoNameKey, daoName, filesKey, rewardKey,
                userIdKey, sessionIdKey, filePath, separatorKey, indexer);
    }
}
