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
    private final List<String> uniqueFields;
    private final String daoConfigKey;
    private final Configuration config;

    @Inject
    private ESBasedIndexerConfig(Configuration daoConfigs,
                                 String elasticSearchIndex,
                                 String indexTypeKey,
                                 String indexType,
                                 List<String> uniqueFields,
                                 Injector injector, String daoConfigKey,
                                 Configuration config) {
        this.config = config;
        this.injector = injector;
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
                indexTypeKey, indexType, uniqueFields, injector, daoConfigKey, config);
    }
}
