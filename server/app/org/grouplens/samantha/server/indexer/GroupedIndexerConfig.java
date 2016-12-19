package org.grouplens.samantha.server.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class GroupedIndexerConfig implements IndexerConfig {
    private final Configuration config;
    private final Injector injector;
    private final Configuration daoConfigs;
    private final String daoConfigKey;
    private final List<String> dataFields;
    private final String daoNameKey;
    private final String daoName;
    private final String filesKey;
    private final String separatorKey;
    private final String indexerName;
    private final String dataDir;
    private final String dataDirKey;
    private final int numBuckets;
    private final List<String> groupKeys;
    private final List<String> orderFields;
    private final Boolean descending;
    private final String separator;

    protected GroupedIndexerConfig(Configuration config, Injector injector, String dataDir,
                                   String indexerName, String dataDirKey, List<String> dataFields,
                                   String daoNameKey, String daoName, String filesKey,
                                   String separatorKey, int numBuckets, List<String> groupKeys,
                                   List<String> orderFields, Boolean descending, String separator,
                                   Configuration daoConfigs, String daoConfigKey) {
        this.config = config;
        this.injector = injector;
        this.dataFields = dataFields;
        this.daoName = daoName;
        this.daoNameKey = daoNameKey;
        this.filesKey = filesKey;
        this.separatorKey = separatorKey;
        this.indexerName = indexerName;
        this.dataDir = dataDir;
        this.dataDirKey = dataDirKey;
        this.numBuckets = numBuckets;
        this.groupKeys = groupKeys;
        this.orderFields = orderFields;
        this.descending = descending;
        this.separator = separator;
        this.daoConfigKey = daoConfigKey;
        this.daoConfigs = daoConfigs;
    }

    public static IndexerConfig getIndexerConfig(Configuration indexerConfig,
                                                 Injector injector) {
        return new GroupedIndexerConfig(indexerConfig, injector,
                indexerConfig.getString("dataDir"), indexerConfig.getString("dependedIndexer"),
                indexerConfig.getString("dataDirKey"), indexerConfig.getStringList("dataFields"),
                indexerConfig.getString("daoNameKey"), indexerConfig.getString("daoName"),
                indexerConfig.getString("filesKey"), indexerConfig.getString("separatorKey"),
                indexerConfig.getInt("numBuckets"), indexerConfig.getStringList("groupKeys"),
                indexerConfig.getStringList("orderFields"), indexerConfig.getBoolean("descending"),
                indexerConfig.getString("separator"),
                indexerConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get()),
                indexerConfig.getString("daoConfigKey"));
    }

    public Indexer getIndexer(RequestContext requestContext) {
        JsonNode reqBody = requestContext.getRequestBody();
        String datDir = JsonHelpers.getOptionalString(reqBody, dataDirKey, dataDir);
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        Indexer indexer = configService.getIndexer(indexerName, requestContext);
        return new GroupedIndexer(configService, config, injector, daoConfigs, daoConfigKey,
                indexer, datDir, numBuckets, groupKeys, dataFields, separator, orderFields, descending,
                filesKey, daoName, daoNameKey, separatorKey);
    }
}
