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

public class AggregateIndexerConfig implements IndexerConfig {
    private final String indexerName;
    private final List<String> otherFields;
    private final String daoNameKey;
    private final String daoName;
    private final String filesKey;
    private final String separator;
    private final String separatorKey;
    private final List<String> groupKeys;
    private final String filePath;
    private final String filePathKey;
    private final Configuration config;
    private final Configuration daoConfigs;
    private final String daoConfigKey;
    private final Injector injector;
    private final List<String> aggFields;
    private final String aggCntName;
    private final String aggSumAppendix;

    private AggregateIndexerConfig(Configuration config, Injector injector, Configuration daoConfigs,
                                    String daoConfigKey, List<String> otherFields,
                                    String daoNameKey, String daoName, String filesKey, String filePathKey,
                                    String separatorKey, String indexerName,
                                    List<String> groupKeys, String filePath, String separator,
                                    String aggCntName, String aggSumAppendix, List<String> aggFields) {
        this.groupKeys = groupKeys;
        this.filePathKey = filePathKey;
        this.filePath = filePath;
        this.otherFields = otherFields;
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
        this.aggCntName = aggCntName;
        this.aggFields = aggFields;
        this.aggSumAppendix = aggSumAppendix;
    }

    public static IndexerConfig getIndexerConfig(Configuration indexerConfig,
                                                 Injector injector) {
        return new AggregateIndexerConfig(indexerConfig, injector,
                indexerConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get()),
                indexerConfig.getString("daoConfigKey"),
                indexerConfig.getStringList("otherFields"),
                indexerConfig.getString("daoNameKey"),
                indexerConfig.getString("daoName"),
                indexerConfig.getString("filesKey"),
                indexerConfig.getString("filePathKey"),
                indexerConfig.getString("separatorKey"),
                indexerConfig.getString("dependedGroupIndexer"),
                indexerConfig.getStringList("groupKeys"),
                indexerConfig.getString("filePath"),
                indexerConfig.getString("separator"),
                indexerConfig.getString("aggCntName"),
                indexerConfig.getString("aggSumAppendix"),
                indexerConfig.getStringList("aggFields")
                );
    }

    public Indexer getIndexer(RequestContext requestContext) {
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        GroupedIndexer indexer = (GroupedIndexer) configService.getIndexer(indexerName, requestContext);
        return new AggregateIndexer(configService, config, injector, daoConfigs, daoConfigKey,
                filePathKey, otherFields, separator, daoNameKey, daoName, filesKey, groupKeys,
                filePath, separatorKey, indexer, aggFields, aggCntName, aggSumAppendix);
    }
}
