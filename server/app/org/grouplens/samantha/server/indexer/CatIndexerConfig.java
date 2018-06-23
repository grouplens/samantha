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

import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class CatIndexerConfig implements IndexerConfig {
    private final Configuration config;
    private final Injector injector;
    private final Configuration daoConfigs;
    private final String daoConfigKey;
    private final String indexersDaoConfigKey;
    private final String daoNameKey;
    private final String daoName;
    private final List<String> indexerNames;

    private CatIndexerConfig(Configuration config, Injector injector, Configuration daoConfigs,
                             String daoConfigKey, String indexersDaoConfigKey, String daoNameKey,
                             String daoName, List<String> indexerNames) {
        this.config = config;
        this.injector = injector;
        this.daoConfigs = daoConfigs;
        this.daoConfigKey = daoConfigKey;
        this.indexersDaoConfigKey = indexersDaoConfigKey;
        this.daoName = daoName;
        this.daoNameKey = daoNameKey;
        this.indexerNames = indexerNames;
    }

    public static IndexerConfig getIndexerConfig(Configuration indexerConfig,
                                                 Injector injector) {
        return new CatIndexerConfig(indexerConfig, injector,
                indexerConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get()),
                indexerConfig.getString("daoConfigKey"),
                indexerConfig.getString("indexerDaosConfigKey"),
                indexerConfig.getString("daoNameKey"),
                indexerConfig.getString("daoName"),
                indexerConfig.getStringList("indexerNames"));
    }

    public Indexer getIndexer(RequestContext requestContext) {
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        return new CatIndexer(configService, config, injector, daoConfigs, daoConfigKey,
                indexersDaoConfigKey, indexerNames, daoName, daoNameKey, 128, requestContext);
    }
}
