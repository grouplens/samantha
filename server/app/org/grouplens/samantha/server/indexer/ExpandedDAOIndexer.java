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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.dao.EntityDAOUtilities;
import org.grouplens.samantha.server.dao.ExpandedEntityDAO;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class ExpandedDAOIndexer extends AbstractIndexer {
    private final String beginTimeKey;
    private final String beginTime;
    private final String endTimeKey;
    private final String endTime;
    private final String daoNameKey;
    private final String daoName;
    private final String filePathKey;
    private final String subDaoName;
    private final String subDaoConfigKey;
    private final String cacheJsonFile;

    public ExpandedDAOIndexer(Configuration config,
                              SamanthaConfigService configService,
                              Injector injector, String daoConfigKey,
                              Configuration daoConfigs, int batchSize,
                              RequestContext requestContext,
                              String beginTime,
                              String beginTimeKey,
                              String endTime,
                              String endTimeKey,
                              String filePathKey,
                              String cacheJsonFile,
                              String daoNameKey,
                              String daoName,
                              String subDaoName,
                              String subDaoConfigKey) {
        super(config, configService, daoConfigs, daoConfigKey, batchSize, requestContext, injector);
        this.beginTime = beginTime;
        this.beginTimeKey = beginTimeKey;
        this.endTime = endTime;
        this.endTimeKey = endTimeKey;
        this.daoName = daoName;
        this.daoNameKey = daoNameKey;
        this.filePathKey = filePathKey;
        this.subDaoConfigKey = subDaoConfigKey;
        this.subDaoName = subDaoName;
        this.cacheJsonFile = cacheJsonFile;
    }

    public ObjectNode getIndexedDataDAOConfig(RequestContext requestContext) {
        JsonNode reqBody = requestContext.getRequestBody();
        try {
            EntityDAO entityDAO = EntityDAOUtilities.getEntityDAO(daoConfigs, requestContext,
                    reqBody.get(daoConfigKey), injector);
            EntityDAO expandedDAO = new ExpandedEntityDAO(expanders, entityDAO, requestContext);
            BufferedWriter writer = new BufferedWriter(new FileWriter(cacheJsonFile));
            while (expandedDAO.hasNextEntity()) {
                IndexerUtilities.writeJson(expandedDAO.getNextEntity(), writer);
            }
            expandedDAO.close();
            writer.close();
        } catch (IOException e) {
            throw new BadRequestException(e);
        }
        String start = JsonHelpers.getOptionalString(reqBody, beginTimeKey,
                beginTime);
        String end = JsonHelpers.getOptionalString(reqBody, endTimeKey,
                endTime);
        int startStamp = IndexerUtilities.parseTime(start);
        int endStamp = IndexerUtilities.parseTime(end);
        ObjectNode sub = Json.newObject();
        sub.set(filePathKey, Json.toJson(cacheJsonFile));
        sub.put(daoNameKey, subDaoName);
        ObjectNode ret = Json.newObject();
        ret.put(daoNameKey, daoName);
        ret.put(beginTimeKey, Integer.valueOf(startStamp).toString());
        ret.put(endTimeKey, Integer.valueOf(endStamp).toString());
        ret.set(subDaoConfigKey, sub);
        return ret;
    }
}
