package org.grouplens.samantha.server.reinforce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.dao.EntityListDAO;
import org.grouplens.samantha.modeler.featurizer.GroupedEntityList;
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

    public UserReturnIndexer(SamanthaConfigService configService,
                             Configuration config, Injector injector, Configuration daoConfigs,
                             String daoConfigKey, String filePathKey,
                             String timestampField, List<String> dataFields, String separator,
                             String daoNameKey, String daoName, String filesKey,
                             String rewardKey, List<String> groupKeys, String sessionIdKey, String filePath,
                             String separatorKey, GroupedIndexer indexer, int maxTime, int reinforceThreshold) {
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
        EntityDAO data = indexer.getEntityDAO(requestContext);
        GroupedEntityList userDao = new GroupedEntityList(groupKeys, data);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
            IndexerUtilities.writeOutHeader(dataFields, writer, separator);
            List<ObjectNode> acts;
            while ((acts = userDao.getNextGroup()).size() > 0) {
                EntityDAO listDao = new EntityListDAO(acts);
                GroupedEntityList grouped = new GroupedEntityList(
                        Lists.newArrayList(sessionIdKey), listDao);
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
                            IndexerUtilities.writeOutJson(entity, dataFields, writer, separator);
                        }
                        lastEntity.put(rewardKey, reward);
                        IndexerUtilities.writeOutJson(lastEntity, dataFields, writer, separator);
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
    public void index(RequestContext requestContext) {
        indexer.index(requestContext);
    }
}
