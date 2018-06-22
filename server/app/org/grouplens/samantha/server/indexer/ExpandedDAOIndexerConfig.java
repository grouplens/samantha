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

public class ExpandedDAOIndexerConfig implements IndexerConfig {
    private final Injector injector;
    private final Configuration config;
    private final Configuration daoConfigs;
    private final String daoConfigKey;
    private final String beginTimeKey;
    private final String beginTime;
    private final String endTimeKey;
    private final String endTime;
    private final String daoNameKey;
    private final String daoName;
    private final String filePathKey;
    private final String subDaoName;
    private final String subDaoConfigKey;
    private final String cacheJsonFile;

    private ExpandedDAOIndexerConfig(Injector injector, Configuration config,
                                     Configuration daoConfigs, String daoConfigKey,
                                     String beginTime,
                                     String beginTimeKey,
                                     String endTime,
                                     String endTimeKey,
                                     String filePathKey,
                                     String cacheJsonFile,
                                     String daoNameKey,
                                     String daoName,
                                     String subDaoName,
                                     String subDaoConfigKey) {
        this.injector = injector;
        this.config = config;
        this.daoConfigKey = daoConfigKey;
        this.daoConfigs = daoConfigs;
        this.beginTime = beginTime;
        this.beginTimeKey = beginTimeKey;
        this.endTime = endTime;
        this.endTimeKey = endTimeKey;
        this.daoName = daoName;
        this.daoNameKey = daoNameKey;
        this.filePathKey = filePathKey;
        this.subDaoConfigKey = subDaoConfigKey;
        this.subDaoName = subDaoName;
        this.cacheJsonFile = cacheJsonFile;
    }

    public static IndexerConfig getIndexerConfig(Configuration indexerConfig,
                                                 Injector injector) {
        return new ExpandedDAOIndexerConfig(injector, indexerConfig,
                indexerConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get()),
                indexerConfig.getString("daoConfigKey"),
                indexerConfig.getString("beginTime"),
                indexerConfig.getString("beginTimeKey"),
                indexerConfig.getString("endTime"),
                indexerConfig.getString("endTimeKey"),
                indexerConfig.getString("filePathKey"),
                indexerConfig.getString("cacheJsonFile"),
                indexerConfig.getString("daoNameKey"),
                indexerConfig.getString("daoName"),
                indexerConfig.getString("subDaoName"),
                indexerConfig.getString("subDaoConfigKey"));
    }

    public Indexer getIndexer(RequestContext requestContext) {
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        return new ExpandedDAOIndexer(config, configService, injector, daoConfigKey,
                daoConfigs, 128, requestContext, beginTime, beginTimeKey,
                endTime, endTimeKey, filePathKey, cacheJsonFile, daoNameKey, daoName,
                subDaoName, subDaoConfigKey);
    }
}
