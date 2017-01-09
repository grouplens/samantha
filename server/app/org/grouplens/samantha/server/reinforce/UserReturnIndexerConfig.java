package org.grouplens.samantha.server.reinforce;

import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.indexer.*;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class UserReturnIndexerConfig implements IndexerConfig {
    private final String indexerName;
    private final String timestampField;
    private final List<String> dataFields;
    private final String daoNameKey;
    private final String daoName;
    private final String filesKey;
    private final String separator;
    private final String separatorKey;
    private final String rewardKey;
    private final List<String> groupKeys;
    private final String sessionIdKey;
    private final String filePath;
    private final String filePathKey;
    private final Configuration config;
    private final Configuration daoConfigs;
    private final String daoConfigKey;
    private final Injector injector;
    private final int reinforceThreshold;

    private UserReturnIndexerConfig(Configuration config, Injector injector, Configuration daoConfigs,
                                    String daoConfigKey, String timestampField, List<String> dataFields,
                                    String daoNameKey, String daoName, String filesKey, String filePathKey,
                                    String separatorKey, String indexerName, String rewardKey,
                                    List<String> groupKeys, String sessionIdKey, String filePath, String separator,
                                    int reinforceThreshold) {
        this.rewardKey = rewardKey;
        this.groupKeys = groupKeys;
        this.filePathKey = filePathKey;
        this.sessionIdKey = sessionIdKey;
        this.filePath = filePath;
        this.timestampField = timestampField;
        this.dataFields = dataFields;
        this.daoName = daoName;
        this.daoNameKey = daoNameKey;
        this.filesKey = filesKey;
        this.separatorKey = separatorKey;
        this.separator = separator;
        this.indexerName = indexerName;
        this.config = config;
        this.daoConfigs = daoConfigs;
        this.daoConfigKey = daoConfigKey;
        this.injector = injector;
        this.reinforceThreshold = reinforceThreshold;
    }

    public static IndexerConfig getIndexerConfig(Configuration indexerConfig,
                                                 Injector injector) {
        return new UserReturnIndexerConfig(indexerConfig, injector,
                indexerConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get()),
                indexerConfig.getString("daoConfigKey"),
                indexerConfig.getString("timestampField"),
                indexerConfig.getStringList("dataFields"),
                indexerConfig.getString("daoNameKey"),
                indexerConfig.getString("daoName"),
                indexerConfig.getString("filesKey"),
                indexerConfig.getString("filePathKey"),
                indexerConfig.getString("separatorKey"),
                indexerConfig.getString("dependedIndexer"),
                indexerConfig.getString("rewardKey"),
                indexerConfig.getStringList("groupKeys"),
                indexerConfig.getString("sessionIdKey"),
                indexerConfig.getString("filePath"),
                indexerConfig.getString("separator"),
                indexerConfig.getInt("reinforceThreshold"));
    }

    public Indexer getIndexer(RequestContext requestContext) {
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        GroupedIndexer indexer = (GroupedIndexer) configService.getIndexer(indexerName, requestContext);
        int maxTime = JsonHelpers.getOptionalInt(requestContext.getRequestBody(), "maxTime",
                (int) (System.currentTimeMillis() / 1000));
        return new UserReturnIndexer(configService, config, injector, daoConfigs, daoConfigKey,
                filePathKey, timestampField, dataFields, separator, daoNameKey, daoName, filesKey, rewardKey,
                groupKeys, sessionIdKey, filePath, separatorKey, indexer, maxTime, reinforceThreshold);
    }
}
