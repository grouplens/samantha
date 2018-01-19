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

package org.grouplens.samantha.server.tensorflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.modeler.instance.GroupedEntityList;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.indexer.AbstractIndexer;
import org.grouplens.samantha.server.indexer.GroupedIndexer;
import org.grouplens.samantha.server.indexer.IndexerUtilities;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class UserSequenceIndexer extends AbstractIndexer {
    private final GroupedIndexer indexer;
    private final List<String> dataFields;
    private final String daoNameKey;
    private final String daoName;
    private final String filesKey;
    private final String innerFieldSeparator;
    private final String separator;
    private final String separatorKey;
    private final List<String> groupKeys;
    private final String filePath;
    private final String filePathKey;
    private final String usedGroupsFilePath;

    public UserSequenceIndexer(SamanthaConfigService configService,
                               Configuration config, Injector injector, Configuration daoConfigs,
                               String daoConfigKey, String filePathKey,
                               List<String> dataFields, String separator,
                               String daoNameKey, String daoName, String filesKey,
                               List<String> groupKeys, String filePath, String innerFieldSeparator,
                               String separatorKey, GroupedIndexer indexer, String usedGroupsFilePath) {
        super(config, configService, daoConfigs, daoConfigKey, injector);
        this.indexer = indexer;
        this.filePathKey = filePathKey;
        this.dataFields = dataFields;
        this.daoName = daoName;
        this.daoNameKey = daoNameKey;
        this.filesKey = filesKey;
        this.innerFieldSeparator = innerFieldSeparator;
        this.separatorKey = separatorKey;
        this.separator = separator;
        this.groupKeys = groupKeys;
        this.filePath = filePath;
        this.usedGroupsFilePath = usedGroupsFilePath;
    }

    public ObjectNode getIndexedDataDAOConfig(RequestContext requestContext) {
        Map<String, Boolean> usedGroups = new HashMap<>();
        IndexerUtilities.loadUsedGroups(usedGroupsFilePath, separator, groupKeys, usedGroups);
        EntityDAO data = indexer.getEntityDAO(requestContext);
        GroupedEntityList userDao = new GroupedEntityList(groupKeys, null, data);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
            List<String> allFields = new ArrayList<>();
            allFields.addAll(groupKeys);
            allFields.addAll(dataFields);
            IndexerUtilities.writeCSVHeader(allFields, writer, separator);
            List<ObjectNode> acts;
            while ((acts = userDao.getNextGroup()).size() > 0) {
                if (usedGroups.size() > 0) {
                    String grpStr = FeatureExtractorUtilities.composeConcatenatedKey(acts.get(0), groupKeys);
                    if (!usedGroups.containsKey(grpStr)) {
                        continue;
                    }
                }
                Map<String, List<String>> field2val = new HashMap<>();
                for (String field : dataFields) {
                    List<String> newVals = new ArrayList<>();
                    field2val.put(field, newVals);
                    for (JsonNode act : acts) {
                        newVals.add(act.get(field).asText());
                    }
                }
                ObjectNode newData = Json.newObject();
                IOUtilities.parseEntityFromJsonNode(groupKeys, acts.get(0), newData);
                for (Map.Entry<String, List<String>> entry : field2val.entrySet()) {
                    newData.put(entry.getKey(), StringUtils.join(entry.getValue(), innerFieldSeparator));
                }
                IndexerUtilities.writeCSVFields(newData, allFields, writer, separator);
                acts.clear();
            }
            writer.close();
        } catch (IOException e) {
            throw new BadRequestException(e);
        }
        data.close();
        ObjectNode ret = Json.newObject();
        ret.put(daoNameKey, daoName);
        String path = JsonHelpers.getOptionalString(requestContext.getRequestBody(), filePathKey, filePath);
        ret.set(filesKey, Json.toJson(Lists.newArrayList(path)));
        ret.put(separatorKey, separator);
        return ret;
    }

    public void index(JsonNode documents, RequestContext requestContext) {
        indexer.index(documents, requestContext);
    }
}
