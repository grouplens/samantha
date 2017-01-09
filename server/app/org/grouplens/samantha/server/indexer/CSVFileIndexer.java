package org.grouplens.samantha.server.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.util.List;

public class CSVFileIndexer extends AbstractIndexer {
    private final CSVFileService dataService;
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
                          CSVFileService dataService,
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
        JsonNode data;
        if (!documents.isArray()) {
            ArrayNode arr = Json.newArray();
            arr.add(documents);
            data = arr;
        } else {
            data = documents;
        }
        for (JsonNode document : data) {
            int tstamp = document.get(timestampField).asInt();
            dataService.write(indexType, document, dataFields, tstamp);
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
