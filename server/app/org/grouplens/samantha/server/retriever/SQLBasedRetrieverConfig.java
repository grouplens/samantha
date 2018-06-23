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

package org.grouplens.samantha.server.retriever;

import org.grouplens.samantha.server.common.AbstractComponentConfig;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import play.Configuration;
import play.db.DB;
import play.inject.Injector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQLBasedRetrieverConfig extends AbstractComponentConfig implements RetrieverConfig {
    private final Injector injector;
    private final String setCursorKey;
    private final Integer limit;
    private final Integer offset;
    private final String selectSqlKey;
    private final List<String> matchFields;
    private final List<String> greaterFields;
    private final List<String> lessFields;
    private final List<String> matchFieldTypes;
    private final List<String> lessFieldTypes;
    private final List<String> greaterFieldTypes;
    private final String db;
    private final List<String> selectFields;
    private final String table;
    private final Map<String, Boolean> orderByFields;
    private final Map<String, String> renameMap;

    private SQLBasedRetrieverConfig(String setCursorKey, Integer limit, Integer offset, String selectSqlKey,
                                    List<String> matchFields, List<String> greaterFields, List<String> lessFields,
                                    List<String> matchFieldTypes, List<String> greaterFieldTypes,
                                    List<String> lessFieldTypes,
                                    String db, List<String> selectFields, Map<String, String> renameMap,
                                    String table, Map<String, Boolean> orderByFields,
                                    Injector injector, Configuration config) {
        super(config);
        this.injector = injector;
        this.setCursorKey = setCursorKey;
        this.limit = limit;
        this.offset = offset;
        this.selectSqlKey = selectSqlKey;
        this.matchFields = matchFields;
        this.greaterFields = greaterFields;
        this.lessFields = lessFields;
        this.matchFieldTypes = matchFieldTypes;
        this.greaterFieldTypes = greaterFieldTypes;
        this.lessFieldTypes = lessFieldTypes;
        this.db = db;
        this.selectFields = selectFields;
        this.table = table;
        this.orderByFields = orderByFields;
        this.renameMap = renameMap;
    }

    public static RetrieverConfig getRetrieverConfig(Configuration retrieverConfig,
                                                     Injector injector) {
        int limit = 500;
        if (retrieverConfig.asMap().containsKey("limit")) {
            limit = retrieverConfig.getInt("limit");
        }
        int offset = 0;
        if (retrieverConfig.asMap().containsKey("offset")) {
            offset = retrieverConfig.getInt("offset");
        }
        Map<String, Boolean> orderByFields = new HashMap<>();
        if (retrieverConfig.asMap().containsKey("orderByFields")) {
            Configuration orderConfig = retrieverConfig.getConfig("orderByFields");
            for (String name : orderConfig.keys()) {
                orderByFields.put(name, orderConfig.getBoolean(name));
            }
        }
        Map<String, String> renameMap = new HashMap<>();
        if (retrieverConfig.asMap().containsKey("renameFields")) {
            Configuration renameConfig = retrieverConfig.getConfig("renameFields");
            for (String name : renameConfig.keys()) {
                renameMap.put(name, renameConfig.getString(name));
            }
        }
        return new SQLBasedRetrieverConfig(retrieverConfig.getString("setCursorKey"), limit, offset,
                retrieverConfig.getString("selectSqlKey"),
                retrieverConfig.getStringList("matchFields"),
                retrieverConfig.getStringList("greaterFields"),
                retrieverConfig.getStringList("lessFields"),
                retrieverConfig.getStringList("matchFieldTypes"),
                retrieverConfig.getStringList("greaterFieldTypes"),
                retrieverConfig.getStringList("lessFieldTypes"),
                retrieverConfig.getString("db"), retrieverConfig.getStringList("selectFields"),
                renameMap, retrieverConfig.getString("table"), orderByFields, injector, retrieverConfig);
    }

    public Retriever getRetriever(RequestContext requestContext) {
        List<EntityExpander> expanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        DSLContext create = DSL.using(DB.getDataSource(db), SQLDialect.DEFAULT);
        return new SQLBasedRetriever(config, expanders, setCursorKey,
                limit, offset, selectSqlKey, matchFields,
                greaterFields, lessFields, matchFieldTypes, greaterFieldTypes, lessFieldTypes,
                create, selectFields, table, orderByFields, renameMap);
    }
}
