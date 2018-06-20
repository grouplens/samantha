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
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import play.Configuration;
import play.db.DB;
import play.inject.Injector;

import java.util.List;

public class SQLBasedIndexerConfig implements IndexerConfig {
    private final Injector injector;
    private final Configuration config;
    private final Configuration daoConfigs;
    private final String daoConfigKey;
    private final String db;
    private final String tableKey;
    private final String table;
    private final List<String> fields;
    private final List<String> fieldTypes;
    private final List<String> matchFields;
    private final List<String> matchFieldTypes;
    private final String retrieverName;
    private final String setCursorKey;
    private final String daoNameKey;
    private final String daoName;
    private final String cacheCsvFile;
    private final String filePathKey;
    private final String separatorKey;

    private SQLBasedIndexerConfig(Injector injector, Configuration config, Configuration daoConfigs,
                                  String daoConfigKey, String db, String tableKey, String table,
                                  List<String> matchFields, List<String> matchFieldTypes,
                                  List<String> fields, List<String> fieldTypes,
                                  String retrieverName, String setCursorKey,
                                  String daoNameKey, String daoName, String cacheCsvFile,
                                  String filePathKey, String separatorKey) {
        this.injector = injector;
        this.config = config;
        this.daoConfigKey = daoConfigKey;
        this.daoConfigs = daoConfigs;
        this.db = db;
        this.table = table;
        this.tableKey = tableKey;
        this.fields = fields;
        this.fieldTypes = fieldTypes;
        this.matchFields = matchFields;
        this.matchFieldTypes = matchFieldTypes;
        this.retrieverName = retrieverName;
        this.setCursorKey = setCursorKey;
        this.daoName = daoName;
        this.daoNameKey = daoNameKey;
        this.cacheCsvFile = cacheCsvFile;
        this.filePathKey = filePathKey;
        this.separatorKey = separatorKey;
    }

    public static IndexerConfig getIndexerConfig(Configuration indexerConfig,
                                                 Injector injector) {
        return new SQLBasedIndexerConfig(injector, indexerConfig,
                indexerConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get()),
                indexerConfig.getString("daoConfigKey"),
                indexerConfig.getString("db"),
                indexerConfig.getString("tableKey"),
                indexerConfig.getString("table"),
                indexerConfig.getStringList("matchFields"),
                indexerConfig.getStringList("matchFieldTypes"),
                indexerConfig.getStringList("fields"),
                indexerConfig.getStringList("fieldTypes"),
                indexerConfig.getString("retrieverName"),
                indexerConfig.getString("setCursorKey"),
                indexerConfig.getString("daoNameKey"),
                indexerConfig.getString("daoName"),
                indexerConfig.getString("cacheCsvFile"),
                indexerConfig.getString("filePathKey"),
                indexerConfig.getString("separatorKey"));
    }

    public Indexer getIndexer(RequestContext requestContext) {
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        DSLContext create = DSL.using(DB.getDataSource(db), SQLDialect.DEFAULT);
        return new SQLBasedIndexer(configService, daoConfigs,
                create, tableKey, table, injector, daoConfigKey,
                fields, fieldTypes, matchFields, matchFieldTypes,
                retrieverName, setCursorKey, daoNameKey, daoName,
                cacheCsvFile, filePathKey, separatorKey,
                config, 128, requestContext);
    }
}
