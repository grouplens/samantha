/*
 * Copyright (c) [2016-2017] [University of Minnesota]
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

import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class CSVFileIndexerConfig implements IndexerConfig {
    private final Configuration config;
    private final String indexType;
    private final Injector injector;
    private final Configuration daoConfigs;
    private final String daoConfigKey;
    private final String timestampField;
    private final List<String> dataFields;
    private final String beginTimeKey;
    private final String beginTime;
    private final String endTimeKey;
    private final String endTime;
    private final String daoNameKey;
    private final String daoName;
    private final String filesKey;
    private final String separatorKey;
    private final String subDaoName;
    private final String subDaoConfigKey;

    protected CSVFileIndexerConfig(Configuration config, Injector injector, Configuration daoConfigs,
                                   String daoConfigKey, String timestampField, List<String> dataFields,
                                   String beginTimeKey, String beginTime, String endTimeKey, String endTime,
                                   String daoNameKey, String daoName, String filesKey,
                                   String separatorKey, String indexType, String subDaoConfigKey,
                                   String subDaoName) {
        this.config = config;
        this.indexType = indexType;
        this.injector = injector;
        this.daoConfigKey = daoConfigKey;
        this.daoConfigs = daoConfigs;
        this.timestampField = timestampField;
        this.dataFields = dataFields;
        this.beginTime = beginTime;
        this.beginTimeKey = beginTimeKey;
        this.endTime = endTime;
        this.endTimeKey = endTimeKey;
        this.daoName = daoName;
        this.daoNameKey = daoNameKey;
        this.filesKey = filesKey;
        this.separatorKey = separatorKey;
        this.subDaoConfigKey = subDaoConfigKey;
        this.subDaoName = subDaoName;
    }

    public static IndexerConfig getIndexerConfig(Configuration indexerConfig,
                                                 Injector injector) {
        return new CSVFileIndexerConfig(indexerConfig, injector,
                indexerConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get()),
                indexerConfig.getString("daoConfigKey"),
                indexerConfig.getString("timestampField"),
                indexerConfig.getStringList("dataFields"),
                indexerConfig.getString("beginTime"),
                indexerConfig.getString("beginTimeKey"),
                indexerConfig.getString("endTime"),
                indexerConfig.getString("endTimeKey"),
                indexerConfig.getString("daoName"),
                indexerConfig.getString("daoNameKey"),
                indexerConfig.getString("filesKey"),
                indexerConfig.getString("separatorKey"),
                indexerConfig.getString("indexType"),
                indexerConfig.getString("subDaoConfigKey"),
                indexerConfig.getString("subDaoName"));
    }

    public Indexer getIndexer(RequestContext requestContext) {
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        CSVFileService dataService = injector.instanceOf(CSVFileService.class);
        return new CSVFileIndexer(configService, dataService, config, injector,
                daoConfigs, daoConfigKey, timestampField, dataFields, beginTime, beginTimeKey,
                endTime, endTimeKey, daoName, daoNameKey, filesKey, separatorKey, indexType,
                subDaoName, subDaoConfigKey);
    }
}
