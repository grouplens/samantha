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

package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.indexer.SQLBasedIndexer;
import org.grouplens.samantha.server.io.RequestContext;
import org.jooq.*;
import org.jooq.impl.DSL;
import play.Configuration;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SQLBasedRetriever extends AbstractRetriever {
    private final List<EntityExpander> expanders;
    private final int limit;
    private final int offset;
    private final String setCursorKey;
    private final String selectSqlKey;
    private final String table;
    private final List<Field<Object>> selectFields;
    private final List<SortField<Object>> orderByFields;
    private final List<String> matchFields;
    private final List<String> lessFields;
    private final List<String> greaterFields;
    private final List<String> matchFieldTypes;
    private final List<String> lessFieldTypes;
    private final List<String> greaterFieldTypes;
    private final Map<String, String> renameMap;
    private final DSLContext create;
    private Cursor<Record> cursor;
    private boolean cursorComplete = false;

    public SQLBasedRetriever(Configuration config, List<EntityExpander> expanders, String setCursorKey,
                             int limit, int offset, String selectSqlKey, List<String> matchFields,
                             List<String> greaterFields, List<String> lessFields, List<String> matchFieldTypes,
                             List<String> greaterFieldTypes, List<String> lessFieldTypes, DSLContext create,
                             List<String> selectFields, String table, Map<String, Boolean> orderByFields,
                             Map<String, String> renameMap) {
        super(config);
        this.expanders = expanders;
        this.offset = offset;
        this.limit = limit;
        this.setCursorKey = setCursorKey;
        this.selectSqlKey = selectSqlKey;
        this.matchFields = matchFields;
        this.greaterFields = greaterFields;
        this.lessFields = lessFields;
        this.matchFieldTypes = matchFieldTypes;
        this.greaterFieldTypes = greaterFieldTypes;
        this.lessFieldTypes = lessFieldTypes;
        this.create = create;
        this.table = table;
        if (selectFields != null && selectFields.size() > 0) {
            this.selectFields = new ArrayList<>(selectFields.size());
            for (String field : selectFields) {
                this.selectFields.add(DSL.field(field));
            }
        } else {
            this.selectFields = null;
        }
        if (orderByFields != null && orderByFields.size() > 0) {
            this.orderByFields = new ArrayList<>(orderByFields.size());
            for (Map.Entry<String, Boolean> field : orderByFields.entrySet()) {
                SortField<Object> sortField = DSL.field(field.getKey()).asc();
                if (field.getValue()) {
                    sortField = DSL.field(field.getKey()).desc();
                }
                this.orderByFields.add(sortField);
            }
        } else {
            this.orderByFields = null;
        }
        this.renameMap = renameMap;
    }

    private List<ObjectNode> parseResult(Result<Record> result) {
        List<ObjectNode> objList = new ArrayList<>(result.size());
        for (Record record : result) {
            ObjectNode obj = Json.newObject();
            for (Field field : record.fields()) {
                String key = field.getName();
                if (renameMap.containsKey(key)) {
                    key = renameMap.get(key);
                }
                obj.put(key, field.getValue(record).toString());
            }
            objList.add(obj);
        }
        return objList;
    }

    public RetrievedResult retrieve(RequestContext requestContext) {
        JsonNode reqBody = requestContext.getRequestBody();
        boolean setCursor = JsonHelpers.getOptionalBoolean(reqBody, setCursorKey, false);
        if (setCursor) {
            if (cursorComplete) {
                return new RetrievedResult(new ArrayList<>(), 0);
            } else if (cursor != null) {
                Result<Record> result = cursor.fetch(limit);
                List<ObjectNode> resultList = parseResult(result);
                resultList = ExpanderUtilities.expand(resultList, expanders, requestContext);
                if (resultList.size() == 0) {
                    cursor.close();
                    cursorComplete = true;
                }
                return new RetrievedResult(resultList, resultList.size());
            }
        }
        String sql;
        if (reqBody.has(selectSqlKey)) {
            sql = JsonHelpers.getRequiredString(reqBody, selectSqlKey);
        } else {
            SelectJoinStep<Record> select = create.select().from(DSL.table(table));
            if (selectFields != null && selectFields.size() > 0) {
                select = create.select(selectFields).from(DSL.table(table));
            }
            Condition conds = null;
            if (matchFields != null && matchFields.size() > 0) {
                for (int i=0; i<matchFields.size(); i++) {
                    String field = matchFields.get(i);
                    String type = matchFieldTypes.get(i);
                    Condition cond = DSL.field(field).eq(SQLBasedIndexer.BasicType
                            .valueOf(type).getValue(field, reqBody));
                    if (conds == null) {
                        conds = cond;
                    } else {
                        conds = conds.and(cond);
                    }
                }
            }
            if (greaterFields != null && greaterFields.size() > 0) {
                for (int i=0; i<greaterFields.size(); i++) {
                    String field = greaterFields.get(i);
                    String type = greaterFieldTypes.get(i);
                    Condition cond = DSL.field(field).greaterThan(SQLBasedIndexer.BasicType
                            .valueOf(type).getValue(field, reqBody));
                    if (conds == null) {
                        conds = cond;
                    } else {
                        conds = conds.and(cond);
                    }
                }
            }
            if (lessFields != null && lessFields.size() > 0) {
                for (int i=0; i<lessFields.size(); i++) {
                    String field = lessFields.get(i);
                    String type = lessFieldTypes.get(i);
                    Condition cond = DSL.field(field).lessThan(SQLBasedIndexer.BasicType
                            .valueOf(type).getValue(field, reqBody));
                    if (conds == null) {
                        conds = cond;
                    } else {
                        conds = conds.and(cond);
                    }
                }
            }
            if (orderByFields != null) {
                SelectSeekStepN<Record> orderBy = select.where(conds).orderBy(orderByFields);
                if (!setCursor) {
                    sql = orderBy.limit(offset, limit).getSQL();
                } else {
                    sql = orderBy.getSQL();
                }
            } else {
                SelectConditionStep<Record> conditionStep = select.where(conds);
                if (!setCursor) {
                    sql = conditionStep.limit(offset, limit).getSQL();
                } else {
                    sql = conditionStep.getSQL();
                }
            }
        }
        Result<Record> result;
        if (setCursor) {
            cursor = create.fetchLazy(sql);
            result = cursor.fetch(limit);
        } else {
            result = create.fetch(sql);
        }
        List<ObjectNode> resultList = parseResult(result);
        ExpanderUtilities.expand(resultList, expanders, requestContext);
        if (setCursor && resultList.size() == 0) {
            cursor.close();
            cursorComplete = true;
        }
        return new RetrievedResult(resultList, resultList.size());
    }
}
