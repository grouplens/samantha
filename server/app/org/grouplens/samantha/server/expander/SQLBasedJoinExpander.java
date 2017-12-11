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

package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.common.Utilities;
import org.grouplens.samantha.server.indexer.SQLBasedIndexer;
import org.grouplens.samantha.server.io.RequestContext;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import play.db.DB;
import play.inject.Injector;
import play.libs.Json;

import java.util.*;

public class SQLBasedJoinExpander implements EntityExpander {
    private static Logger logger = LoggerFactory.getLogger(SQLBasedJoinExpander.class);
    final private List<Configuration> configList;
    final private DSLContext create;

    public SQLBasedJoinExpander(List<Configuration> configList, DSLContext create) {
        this.configList = configList;
        this.create = create;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector,
                                             RequestContext requestContext) {
        DSLContext create = DSL.using(DB.getDataSource(expanderConfig.getString("db")), SQLDialect.DEFAULT);
        return new SQLBasedJoinExpander(expanderConfig.getConfigList("expandFields"), create);
    }

    private Result<Record> bulkUniqueGetWithSQL(String table, List<String> keys,
                                                List<String> keyTypes,
                                                List<ObjectNode> initial,
                                                List<String> entityFields) {
        List<Field<Object>> target = new ArrayList<>();
        for (String field : entityFields) {
            target.add(DSL.field(field));
        }
        for (String field : keys) {
            target.add(DSL.field(field));
        }
        SelectJoinStep<Record> query = create.select(target).from(table);
        if (keys.size() == 1) {
            Set<Object> uniqKeys = new HashSet<>();
            for (JsonNode entity : initial) {
                uniqKeys.add(SQLBasedIndexer.BasicType.valueOf(keyTypes.get(0)).getValue(keys.get(0), entity));
            }
            Field<Object> field = DSL.field(keys.get(0));
            return query.where(field.in(uniqKeys)).fetch();
        } else {
            Set<String> uniqKeys = new HashSet<>();
            Condition condition = null;
            for (JsonNode entity : initial) {
                String key = Utilities.composeKey(entity, keys);
                if (!uniqKeys.contains(key)) {
                    uniqKeys.add(key);
                    Condition one = null;
                    for (int i=0; i<keys.size(); i++) {
                        Condition specific = DSL.field(keys.get(0))
                                .eq(SQLBasedIndexer.BasicType.valueOf(keyTypes.get(i))
                                        .getValue(keys.get(i), entity));
                        if (one == null) {
                            one = specific;
                        } else {
                            one = one.and(specific);
                        }
                    }
                    if (condition == null) {
                        condition = one;
                    } else {
                        condition.or(one);
                    }
                }
            }
            return query.where(condition).fetch();
        }
    }

    private void parseEntityFromRecord(List<String> fields, List<String> fieldTypes,
                                       Record record, ObjectNode entity) {
        for (int i=0; i<fields.size(); i++) {
            SQLBasedIndexer.BasicType.valueOf(fieldTypes.get(i)).putValue(fields.get(i),
                    entity, record.get(fields.get(i)));
        }
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        if (initialResult.size() == 0) {
            return initialResult;
        }
        for (Configuration config : configList) {
            String table = config.getString("table");
            List<String> keys = config.getStringList("keys");
            List<String> keyTypes = config.getStringList("keyTypes");
            List<String> entityFields = config.getStringList("fields");
            List<String> fieldTypes = config.getStringList("fieldTypes");
            Result<Record> retrieved = bulkUniqueGetWithSQL(table, keys, keyTypes, initialResult, entityFields);
            Map<String, List<Record>> key2val = new HashMap<>();
            for (Record entity : retrieved) {
                ObjectNode jsonEntity = Json.newObject();
                parseEntityFromRecord(keys, keyTypes, entity, jsonEntity);
                String key = Utilities.composeKey(jsonEntity, keys);
                if (key2val.containsKey(key)) {
                    key2val.get(key).add(entity);
                } else {
                    List<Record> list = new ArrayList<>();
                    list.add(entity);
                    key2val.put(key, list);
                }
            }
            for (ObjectNode entity : initialResult) {
                String key = Utilities.composeKey(entity, keys);
                if (key2val.containsKey(key)) {
                    for (Record val : key2val.get(key)) {
                        parseEntityFromRecord(entityFields, fieldTypes, val, entity);
                    }
                } else {
                    logger.warn("Can not find the key {} for {} while joining.", key, entity.toString());
                }
            }
        }
        return initialResult;
    }
}
