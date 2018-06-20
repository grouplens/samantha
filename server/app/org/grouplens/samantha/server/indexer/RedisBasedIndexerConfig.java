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

import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.common.RedisService;

import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class RedisBasedIndexerConfig implements IndexerConfig {
    private final Configuration config;
    private final Injector injector;
    private final String keyFieldsKey;
    private final List<String> keyFields;
    private final String hashFieldsKey;
    private final List<String> hashFields;
    private final String sortFieldKey;
    private final String sortField;
    private final String indexPrefixKey;
    private final String indexPrefix;
    private final String indexStructureKey;
    private final String indexStructure;
    private final String daoConfigKey;
    private final Configuration daoConfigs;

    private RedisBasedIndexerConfig(
                                    String keyFieldsKey, String sortFieldKey, String indexPrefixKey,
                                    String hashFieldsKey, Configuration daoConfigs, Injector injector,
                                    String indexStructureKey, List<String> keyFields, List<String> hashFields,
                                    String sortField, String indexPrefix, String indexStructure,
                                    String daoConfigKey, Configuration config) {
        this.keyFieldsKey = keyFieldsKey;
        this.sortFieldKey = sortFieldKey;
        this.indexPrefixKey = indexPrefixKey;
        this.daoConfigs = daoConfigs;
        this.injector = injector;
        this.hashFieldsKey = hashFieldsKey;
        this.indexStructureKey = indexStructureKey;
        this.daoConfigKey = daoConfigKey;
        this.keyFields = keyFields;
        this.hashFields = hashFields;
        this.sortField = sortField;
        this.indexPrefix = indexPrefix;
        this.indexStructure = indexStructure;
        this.config = config;
    }

    public static IndexerConfig getIndexerConfig(Configuration indexerConfig,
                                          Injector injector) {
        return new RedisBasedIndexerConfig(indexerConfig.getString("keyFieldsKey"),
                indexerConfig.getString("sortFieldKey"), indexerConfig.getString("indexPrefixKey"),
                indexerConfig.getString("hashFieldsKey"),
                indexerConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get()), injector,
                indexerConfig.getString("indexStructureKey"),
                indexerConfig.getStringList("keyFields"),
                indexerConfig.getStringList("hashFields"),
                indexerConfig.getString("sortField"),
                indexerConfig.getString("indexPrefix"),
                indexerConfig.getString("indexStructure"),
                indexerConfig.getString("daoConfigKey"), indexerConfig);
    }

    public Indexer getIndexer(RequestContext requestContext) {
        RedisService redisService = injector.instanceOf(RedisService.class);
        String indexStructure = JsonHelpers.getOptionalString(requestContext.getRequestBody(),
                indexStructureKey, this.indexStructure);
        RedisBasedIndexer.IndexStructure structure = RedisBasedIndexer.IndexStructure.HASH_SET;
        if (indexStructure != null) {
            structure = RedisBasedIndexer.IndexStructure.valueOf(indexStructure);
        }
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        return new RedisBasedIndexer(redisService, structure, configService, keyFieldsKey, sortFieldKey,
                indexPrefixKey, hashFieldsKey, daoConfigs, injector, keyFields, hashFields, sortField,
                indexPrefix, daoConfigKey, config, 128, requestContext);
    }
}
