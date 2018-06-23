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
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
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
    private final int usedBuckets;
    private final List<String> groupKeys;
    private final List<String> orderFields;
    private final List<String> groupKeysTypes;
    private final List<String> orderFieldsTypes;
    private final Boolean descending;
    private final String separator;
    private final boolean skip;

    private GroupedIndexerConfig(Configuration config, Injector injector, String dataDir,
                                 String indexerName, String dataDirKey, List<String> dataFields,
                                 String daoNameKey, String daoName, String filesKey,
                                 String separatorKey, int numBuckets, List<String> groupKeys,
                                 List<String> orderFields, Boolean descending, String separator,
                                 Configuration daoConfigs, String daoConfigKey, int usedBuckets, boolean skip,
                                 List<String> groupKeysTypes, List<String> orderFieldsTypes) {
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
        this.skip = skip;
        this.groupKeys = groupKeys;
        this.orderFields = orderFields;
        this.descending = descending;
        this.separator = separator;
        this.daoConfigKey = daoConfigKey;
        this.daoConfigs = daoConfigs;
        this.usedBuckets = usedBuckets;
        this.orderFieldsTypes = orderFieldsTypes;
        this.groupKeysTypes = groupKeysTypes;
    }

    public static IndexerConfig getIndexerConfig(Configuration indexerConfig,
                                                 Injector injector) {
        int numBuckets = indexerConfig.getInt("numBuckets");
        int usedBuckets = numBuckets;
        if (indexerConfig.asMap().containsKey("usedBuckets")) {
            usedBuckets = indexerConfig.getInt("usedBuckets");
        }
        List<String> groupKeys = indexerConfig.getStringList("groupKeys");
        List<String> orderFields = indexerConfig.getStringList("orderFields");
        List<String> groupKeysTypes = indexerConfig.getStringList("groupKeysTypes");
        if (groupKeysTypes == null) {
            groupKeysTypes = new ArrayList<>();
            for (int i=0; i<groupKeys.size(); i++) {
                groupKeysTypes.add("TEXT");
            }
        }
        List<String> orderFieldsTypes = indexerConfig.getStringList("orderFieldsTypes");
        if (orderFieldsTypes == null) {
            orderFieldsTypes = new ArrayList<>();
            for (int i=0; i<orderFields.size(); i++) {
                orderFieldsTypes.add("TEXT");
            }
        }
        Boolean skip = indexerConfig.getBoolean("skip");
        if (skip == null) {
            skip = false;
        }
        return new GroupedIndexerConfig(indexerConfig, injector,
                indexerConfig.getString("dataDir"), indexerConfig.getString("dependedIndexer"),
                indexerConfig.getString("dataDirKey"), indexerConfig.getStringList("dataFields"),
                indexerConfig.getString("daoNameKey"), indexerConfig.getString("daoName"),
                indexerConfig.getString("filesKey"), indexerConfig.getString("separatorKey"),
                numBuckets, groupKeys, orderFields, indexerConfig.getBoolean("descending"),
                indexerConfig.getString("separator"),
                indexerConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get()),
                indexerConfig.getString("daoConfigKey"), usedBuckets, skip,
                groupKeysTypes, orderFieldsTypes);
    }

    public Indexer getIndexer(RequestContext requestContext) {
        JsonNode reqBody = requestContext.getRequestBody();
        String datDir = JsonHelpers.getOptionalString(reqBody, dataDirKey, dataDir);
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        Indexer indexer = configService.getIndexer(indexerName, requestContext);
        return new GroupedIndexer(configService, config, injector, daoConfigs, daoConfigKey,
                indexer, datDir, numBuckets, groupKeys, dataFields, separator, orderFields, descending,
                filesKey, daoName, daoNameKey, separatorKey, usedBuckets, skip, groupKeysTypes, orderFieldsTypes,
                128, requestContext);
    }
}
