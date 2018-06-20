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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.common.DataOperation;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.dao.RetrieverBasedDAO;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.RequestContext;
import org.jooq.*;
import org.jooq.impl.DSL;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SQLBasedIndexer extends AbstractIndexer {
    private final String tableKey;
    private final String table;
    private final List<String> fieldNames;
    private final List<Field<Object>> fields;
    private final List<Field<Object>> matchFields;
    private final List<String> fieldTypes;
    private final List<String> matchFieldTypes;
    private final DSLContext create;
    private final String retrieverName;
    private final String setCursorKey;
    private final String daoNameKey;
    private final String daoName;
    private final String cacheCsvFile;
    private final String filePathKey;
    private final String separatorKey;

    public SQLBasedIndexer(SamanthaConfigService configService,
                           Configuration daoConfigs,
                           DSLContext create, String tableKey, String table,
                           Injector injector, String daoConfigKey,
                           List<String> fields, List<String> fieldTypes,
                           List<String> matchFields, List<String> matchFieldTypes,
                           String retrieverName, String setCursorKey,
                           String daoNameKey, String daoName, String cacheCsvFile,
                           String filePathKey, String separatorKey,
                           Configuration config, int batchSize, RequestContext requestContext) {
        super(config, configService, daoConfigs, daoConfigKey, batchSize, requestContext, injector);
        this.table = table;
        this.tableKey = tableKey;
        this.fieldNames = fields;
        this.fields = new ArrayList<>(fields.size());
        for (String field : fields) {
            this.fields.add(DSL.field(field));
        }
        this.fieldTypes = fieldTypes;
        this.matchFields = new ArrayList<>(matchFields.size());
        if (matchFields != null) {
            for (String field : matchFields) {
                this.matchFields.add(DSL.field(field));
            }
        }
        this.matchFieldTypes = matchFieldTypes;
        this.create = create;
        this.retrieverName = retrieverName;
        this.setCursorKey = setCursorKey;
        this.daoName = daoName;
        this.daoNameKey = daoNameKey;
        this.cacheCsvFile = cacheCsvFile;
        this.filePathKey = filePathKey;
        this.separatorKey = separatorKey;
    }

    public enum BasicType implements GetValue {
        TEXT() {
            public Object getValue(String name, JsonNode data) {
                return data.get(name).asText();
            }
            public void putValue(String name, ObjectNode data, Object value) {
                data.put(name, (String) value);
            }
            public int compareValue(String name, JsonNode left, JsonNode right) {
                return left.get(name).asText().compareTo(right.get(name).asText());
            }
        },
        INT() {
            public Object getValue(String name, JsonNode data) {
                return data.get(name).asInt();
            }
            public void putValue(String name, ObjectNode data, Object value) {
                data.put(name, (Integer) value);
            }
            public int compareValue(String name, JsonNode left, JsonNode right) {
                return Integer.compare(left.get(name).asInt(), right.get(name).asInt());
            }
        },
        LONG() {
            public Object getValue(String name, JsonNode data) {
                return data.get(name).asLong();
            }
            public void putValue(String name, ObjectNode data, Object value) {
                data.put(name, (Long) value);
            }
            public int compareValue(String name, JsonNode left, JsonNode right) {
                return Long.compare(left.get(name).asLong(), right.get(name).asLong());
            }
        },
        FLOAT() {
            public Object getValue(String name, JsonNode data) {
                return data.get(name).asDouble();
            }
            public void putValue(String name, ObjectNode data, Object value) {
                data.put(name, (Float) value);
            }
            public int compareValue(String name, JsonNode left, JsonNode right) {
                return Double.compare(left.get(name).asDouble(), right.get(name).asDouble());
            }
        },
        DOUBLE() {
            public Object getValue(String name, JsonNode data) {
                return data.get(name).asDouble();
            }
            public void putValue(String name, ObjectNode data, Object value) {
                data.put(name, (Double) value);
            }
            public int compareValue(String name, JsonNode left, JsonNode right) {
                return Double.compare(left.get(name).asDouble(), right.get(name).asDouble());
            }
        }
    }

    private interface GetValue {
        Object getValue(String name, JsonNode data);
        void putValue(String name, ObjectNode data, Object value);
        int compareValue(String name, JsonNode left, JsonNode right);
    }

    private void bulkInsert(String table, JsonNode data) {
        List<Query> batch = new ArrayList<>();
        for (JsonNode point : data) {
            List<Object> values = new ArrayList<>();
            for (int i = 0; i < fieldTypes.size(); i++) {
                values.add(BasicType.valueOf(fieldTypes.get(i)).getValue(fields.get(i).getName(), point));
            }
            InsertValuesStepN<Record> insert = create.insertInto(DSL.table(table))
                    .columns(fields).values(values);
            batch.add(insert);
        }
        create.batch(batch).execute();
    }

    public void bulkDelete(String table, JsonNode data) {
        List<Query> batch = new ArrayList<>();
        for (JsonNode point : data) {
            Condition conds = null;
            for (int i = 0; i < matchFields.size(); i++) {
                String type = matchFieldTypes.get(i);
                Condition cond = matchFields.get(i).eq(SQLBasedIndexer.BasicType
                        .valueOf(type).getValue(matchFields.get(i).getName(), point));
                if (conds == null) {
                    conds = cond;
                } else {
                    conds = conds.and(cond);
                }
            }
            batch.add(create.deleteFrom(DSL.table(table)).where(conds));
        }
        create.batch(batch).execute();
    }

    public void index(JsonNode data, RequestContext requestContext) {
        if (!data.isArray()) {
            ArrayNode arr = Json.newArray();
            arr.add(data);
            data = arr;
        }
        JsonNode reqBody = requestContext.getRequestBody();
        String table = JsonHelpers.getOptionalString(reqBody, tableKey, this.table);
        String operation = JsonHelpers.getOptionalString(reqBody, ConfigKey.DATA_OPERATION.get(),
                DataOperation.INSERT.get());
        if (operation.equals(DataOperation.DELETE.get()) || operation.equals(DataOperation.UPSERT.get())) {
            bulkDelete(table, data);
        }
        if (operation.equals(DataOperation.INSERT.get()) || operation.equals(DataOperation.UPSERT.get())) {
            bulkInsert(table, data);
        }
    }

    public ObjectNode getIndexedDataDAOConfig(RequestContext requestContext) {
        ObjectNode ret = Json.newObject();
        ret.put(daoNameKey, daoName);
        if (cacheCsvFile == null) {
            ret.put("retrieverName", retrieverName);
            ret.put("setCursorKey", setCursorKey);
        } else {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(cacheCsvFile));
                IndexerUtilities.writeCSVHeader(fieldNames, writer, "\t");
                SamanthaConfigService configService = injector
                        .instanceOf(SamanthaConfigService.class);
                EntityDAO dao = new RetrieverBasedDAO(retrieverName, configService, requestContext);
                while (dao.hasNextEntity()) {
                    IndexerUtilities.writeCSVFields(dao.getNextEntity(), fieldNames, writer, "\t");
                }
                dao.close();
            } catch (IOException e) {
                throw new BadRequestException(e);
            }
            ret.put(filePathKey, cacheCsvFile);
            ret.put(separatorKey, "\t");
        }
        return ret;
    }
}
