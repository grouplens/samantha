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

package org.grouplens.samantha.server.reinforce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.grouplens.samantha.modeler.dao.CSVFileDAO;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.dao.EntityListDAO;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.modeler.instance.GroupedEntityList;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.indexer.*;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class UserReturnIndexer extends AbstractIndexer {
    private final GroupedIndexer indexer;
    private final String timestampField;
    private final List<String> dataFields;
    private final String daoNameKey;
    private final String daoName;
    private final String filesKey;
    private final String separator;
    private final String separatorKey;
    private final String rewardKey;
    private final List<String> groupKeys;
    private final String sessionIdKey;
    private final String filePath;
    private final String filePathKey;
    private final int maxTime;
    private final int reinforceThreshold;
    private final String usedGroupsFilePath;

    public UserReturnIndexer(SamanthaConfigService configService,
                             Configuration config, Injector injector, Configuration daoConfigs,
                             String daoConfigKey, String filePathKey,
                             String timestampField, List<String> dataFields, String separator,
                             String daoNameKey, String daoName, String filesKey,
                             String rewardKey, List<String> groupKeys, String sessionIdKey, String filePath,
                             String separatorKey, GroupedIndexer indexer, int maxTime, int reinforceThreshold,
                             String usedGroupsFilePath) {
        super(config, configService, daoConfigs, daoConfigKey, injector);
        this.indexer = indexer;
        this.filePathKey = filePathKey;
        this.timestampField = timestampField;
        this.dataFields = dataFields;
        this.daoName = daoName;
        this.daoNameKey = daoNameKey;
        this.filesKey = filesKey;
        this.separatorKey = separatorKey;
        this.separator = separator;
        this.rewardKey = rewardKey;
        this.groupKeys = groupKeys;
        this.sessionIdKey = sessionIdKey;
        this.filePath = filePath;
        this.maxTime = maxTime;
        this.reinforceThreshold = reinforceThreshold;
        this.usedGroupsFilePath = usedGroupsFilePath;
    }

    private double rewardFunc(double returnTime) {
        if (returnTime == 0) {
            return 1.0;
        }
        return Math.min(1.0, 24 * 3600 / returnTime);
    }

    private double getReward(double lastTime, double maxTime, double newTime) {
        if (newTime - lastTime > reinforceThreshold) {
            if (maxTime - lastTime < reinforceThreshold) {
                return -1.0;
            }
            return 0.0;
        } else {
            return rewardFunc(newTime - lastTime);
        }
    }

    public ObjectNode getIndexedDataDAOConfig(RequestContext requestContext) {
        Map<String, Boolean> usedGroups = new HashMap<>();
        IndexerUtilities.loadUsedGroups(usedGroupsFilePath, separator, groupKeys, usedGroups);
        EntityDAO data = indexer.getEntityDAO(requestContext);
        GroupedEntityList userDao = new GroupedEntityList(groupKeys, null, data);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
            IndexerUtilities.writeCSVHeader(dataFields, writer, separator);
            List<ObjectNode> acts;
            while ((acts = userDao.getNextGroup()).size() > 0) {
                if (usedGroups.size() > 0) {
                    String grpStr = FeatureExtractorUtilities.composeConcatenatedKey(acts.get(0), groupKeys);
                    if (!usedGroups.containsKey(grpStr)) {
                        continue;
                    }
                }
                EntityDAO listDao = new EntityListDAO(acts);
                GroupedEntityList grouped = new GroupedEntityList(
                        Lists.newArrayList(sessionIdKey), null, listDao);
                List<ObjectNode> group = grouped.getNextGroup();
                List<ObjectNode> nextGrp;
                while ((nextGrp = grouped.getNextGroup()).size() > 0) {
                    ObjectNode lastEntity = group.get(group.size() - 1);
                    int lastTime = lastEntity.get(timestampField).asInt();
                    int newTime = nextGrp.get(nextGrp.size() - 1).get(timestampField).asInt();
                    double reward = getReward(lastTime, maxTime, newTime);
                    if (reward >= 0.0) {
                        for (ObjectNode entity : group) {
                            entity.put(rewardKey, 0.0);
                            IndexerUtilities.writeCSVFields(entity, dataFields, writer, separator);
                        }
                        lastEntity.put(rewardKey, reward);
                        IndexerUtilities.writeCSVFields(lastEntity, dataFields, writer, separator);
                    }
                    group.clear();
                    group = nextGrp;
                }
                acts.clear();
                listDao.close();
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
