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

package org.grouplens.samantha.server.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.ConfigRenderOptions;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.EngineComponent;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.dao.EntityDAOUtilities;
import org.grouplens.samantha.server.dao.ExpandedEntityDAO;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.util.List;

abstract public class AbstractIndexer implements Indexer {
    protected final Configuration config;
    protected final SamanthaConfigService configService;
    protected final Configuration daoConfigs;
    protected final String daoConfigKey;
    protected final Injector injector;
    protected final List<EntityExpander> expanders;
    protected final List<EntityExpander> postExpanders;
    protected final List<Configuration> subscribers;
    protected final int batchSize;

    public AbstractIndexer(Configuration config, SamanthaConfigService configService,
                           Configuration daoConfigs, String daoConfigKey, int batchSize,
                           RequestContext requestContext, Injector injector) {
        this.config = config;
        this.configService = configService;
        this.daoConfigKey = daoConfigKey;
        this.daoConfigs = daoConfigs;
        this.injector = injector;
        this.batchSize = batchSize;
        this.expanders = ExpanderUtilities.getEntityExpanders(requestContext,
                ExpanderUtilities.getEntityExpandersConfig(config), injector);
        this.postExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                ExpanderUtilities.getPostEntityExpandersConfig(config), injector);
        this.subscribers = config.getConfigList(ConfigKey.DATA_SUBSCRIBERS.get());
    }

    public Configuration getConfig() {
        return this.config;
    }

    private void notifyDataSubscribers(JsonNode entities, RequestContext requestContext) {
        if (subscribers == null) {
            return;
        }
        ObjectNode reqBody = Json.newObject();
        IOUtilities.parseEntityFromJsonNode(requestContext.getRequestBody(), reqBody);
        ObjectNode daoConfig = Json.newObject();
        daoConfig.put(ConfigKey.ENTITY_DAO_NAME_KEY.get(), ConfigKey.REQUEST_ENTITY_DAO_NAME.get());
        daoConfig.set(ConfigKey.REQUEST_ENTITY_DAO_ENTITIES_KEY.get(), entities);
        reqBody.set(daoConfigKey, daoConfig);
        RequestContext pseudoReq = new RequestContext(reqBody, requestContext.getEngineName());
        for (Configuration configuration : subscribers) {
            String name = configuration.getString(ConfigKey.ENGINE_COMPONENT_NAME.get());
            String type = configuration.getString(ConfigKey.ENGINE_COMPONENT_TYPE.get());
            JsonNode configReq = Json.parse(configuration.getConfig(ConfigKey.REQUEST_CONTEXT.get())
                    .underlying().root().render(ConfigRenderOptions.concise()));
            IOUtilities.parseEntityFromJsonNode(configReq, reqBody);
            EngineComponent.valueOf(type).getComponent(configService, name, pseudoReq);
        }
    }

    public void index(RequestContext requestContext) {
        JsonNode reqBody = requestContext.getRequestBody();
        EntityDAO entityDAO = EntityDAOUtilities.getEntityDAO(daoConfigs, requestContext,
                reqBody.get(daoConfigKey), injector);
        ArrayNode toIndex = Json.newArray();
        ExpandedEntityDAO expandedEntityDAO = new ExpandedEntityDAO(expanders, entityDAO, requestContext);
        while (expandedEntityDAO.hasNextEntity()) {
            toIndex.add(expandedEntityDAO.getNextEntity());
            if (toIndex.size() >= batchSize) {
                index(toIndex, requestContext);
                notifyDataSubscribers(toIndex, requestContext);
                toIndex.removeAll();
            }
        }
        if (toIndex.size() > 0) {
            index(toIndex, requestContext);
            notifyDataSubscribers(toIndex, requestContext);
        }
        expandedEntityDAO.close();
        entityDAO.close();
    }

    public EntityDAO getEntityDAO(RequestContext requestContext) {
        return new ExpandedEntityDAO(postExpanders,
                EntityDAOUtilities.getEntityDAO(daoConfigs, requestContext,
                        getIndexedDataDAOConfig(requestContext), injector), requestContext);
    }

    public ObjectNode getIndexedDataDAOConfig(RequestContext requestContext) {
        throw new BadRequestException("Reading data from this indexer is not supported.");
    }

    public void index(JsonNode data, RequestContext requestContext) {
        throw new BadRequestException("Indexing data into this indexer is not supported.");
    }
}
