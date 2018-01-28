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
import org.grouplens.samantha.server.common.DataOperation;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.util.List;

public class CSVFileIndexer extends AbstractIndexer {
    private static Logger logger = LoggerFactory.getLogger(CSVFileIndexer.class);
    private final FileWriterService dataService;
    private final String indexType;
    private final String timestampField;
    private final List<String> dataFields;
    private final String beginTimeKey;
    private final String beginTime;
    private final String endTimeKey;
    private final String endTime;
    private final String daoNameKey;
    private final String daoName;
    private final String filesKey;
    private final String separatorKey;
    private final String subDaoName;
    private final String subDaoConfigKey;

    public CSVFileIndexer(SamanthaConfigService configService,
                          FileWriterService dataService,
                          Configuration config, Injector injector, Configuration daoConfigs,
                          String daoConfigKey, String timestampField, List<String> dataFields,
                          String beginTimeKey, String beginTime, String endTimeKey, String endTime,
                          String daoNameKey, String daoName, String filesKey,
                          String separatorKey, String indexType, String subDaoName, String subDaoConfigKey) {
        super(config, configService, daoConfigs, daoConfigKey, injector);
        this.dataService = dataService;
        this.indexType = indexType;
        this.timestampField = timestampField;
        this.dataFields = dataFields;
        this.beginTime = beginTime;
        this.beginTimeKey = beginTimeKey;
        this.endTime = endTime;
        this.endTimeKey = endTimeKey;
        this.daoName = daoName;
        this.daoNameKey = daoNameKey;
        this.filesKey = filesKey;
        this.separatorKey = separatorKey;
        this.subDaoConfigKey = subDaoConfigKey;
        this.subDaoName = subDaoName;
    }

    public void index(JsonNode documents, RequestContext requestContext) {
        JsonNode reqBody = requestContext.getRequestBody();
        String operation = JsonHelpers.getOptionalString(reqBody, ConfigKey.DATA_OPERATION.get(),
                DataOperation.INSERT.get());
        if (operation.equals(DataOperation.INSERT.get()) || operation.equals(DataOperation.UPSERT.get())) {
            JsonNode arr;
            if (!documents.isArray()) {
                ArrayNode tmp = Json.newArray();
                tmp.add(documents);
                arr = tmp;
            } else {
                arr = documents;
            }
            int timestamp = (int) (System.currentTimeMillis() / 1000);
            for (JsonNode document : arr) {
                if (document.has(timestampField)) {
                    timestamp = document.get(timestampField).asInt();
                } else {
                    logger.warn("Time field {} is not present in the entity to be indexed.", timestampField);
                }
                dataService.writeCSV(indexType, document, dataFields, timestamp);
            }
        } else {
            throw new BadRequestException("Data operation " + operation + " is not supported");
        }
    }

    public ObjectNode getIndexedDataDAOConfig(RequestContext requestContext) {
        JsonNode reqBody = requestContext.getRequestBody();
        String start = JsonHelpers.getOptionalString(reqBody, beginTimeKey,
                beginTime);
        String end = JsonHelpers.getOptionalString(reqBody, endTimeKey,
                endTime);
        int startStamp = IndexerUtilities.parseTime(start);
        int endStamp = IndexerUtilities.parseTime(end);
        List<String> files = dataService.getFiles(indexType, startStamp, endStamp);
        ObjectNode sub = Json.newObject();
        sub.set(filesKey, Json.toJson(files));
        sub.put(separatorKey, dataService.getSeparator());
        sub.put(daoNameKey, subDaoName);

        ObjectNode ret = Json.newObject();
        ret.put(daoNameKey, daoName);
        ret.put(beginTimeKey, Integer.valueOf(startStamp).toString());
        ret.put(endTimeKey, Integer.valueOf(endStamp).toString());
        ret.set(subDaoConfigKey, sub);
        return ret;
    }
}
