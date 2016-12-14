package org.grouplens.samantha.server.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.ConfigRenderOptions;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.EngineComponent;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.dao.EntityDAOUtilities;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

abstract public class AbstractIndexer implements Indexer {
    protected final Configuration config;
    protected final SamanthaConfigService configService;
    protected final Configuration daoConfigs;
    protected final String daoConfigKey;
    protected final Injector injector;
    //TODO: do the similar thing to expanders for all other components, i.e. moving into abstract
    protected final List<Configuration> expandersConfig;
    protected final List<Configuration> subscribers;
    private final int bufferSize = 100;

    public AbstractIndexer(Configuration config, SamanthaConfigService configService,
                           Configuration daoConfigs, String daoConfigKey, Injector injector) {
        this.config = config;
        this.configService = configService;
        this.daoConfigKey = daoConfigKey;
        this.daoConfigs = daoConfigs;
        this.injector = injector;
        this.expandersConfig = ExpanderUtilities.getEntityExpandersConfig(config);
        this.subscribers = config.getConfigList(ConfigKey.DATA_SUBSCRIBERS.get());
    }

    public Configuration getConfig() {
        return this.config;
    }

    public void notifyDataSubscribers(RequestContext requestContext) {
        if (subscribers == null) {
            return;
        }
        for (Configuration configuration : subscribers) {
            String name = configuration.getString(ConfigKey.ENGINE_COMPONENT_NAME.get());
            String type = configuration.getString(ConfigKey.ENGINE_COMPONENT_TYPE.get());
            JsonNode configReq = Json.parse(configuration.getConfig(ConfigKey.REQUEST_CONTEXT.get())
                    .underlying().root().render(ConfigRenderOptions.concise()));
            ObjectNode reqBody = Json.newObject();
            IOUtilities.parseEntityFromJsonNode(requestContext.getRequestBody(), reqBody);
            IOUtilities.parseEntityFromJsonNode(configReq, reqBody);
            RequestContext pseudoReq = new RequestContext(reqBody, requestContext.getEngineName());
            EngineComponent.valueOf(type).getComponent(configService, name, pseudoReq);
        }
    }

    public void index(RequestContext requestContext) {
        JsonNode reqBody = requestContext.getRequestBody();
        EntityDAO entityDAO = EntityDAOUtilities.getEntityDAO(daoConfigs, requestContext,
                reqBody.get(daoConfigKey), injector);
        List<ObjectNode> bufferArr = new ArrayList<>();
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        while (entityDAO.hasNextEntity()) {
            bufferArr.add(entityDAO.getNextEntity());
            if (bufferArr.size() >= bufferSize) {
                bufferArr = ExpanderUtilities.expand(bufferArr, entityExpanders, requestContext);
                for (ObjectNode entity : bufferArr) {
                    index(entity, requestContext);
                }
                bufferArr.clear();
            }
        }
        bufferArr = ExpanderUtilities.expand(bufferArr, entityExpanders, requestContext);
        for (ObjectNode entity : bufferArr) {
            index(entity, requestContext);
        }
    }

    public ObjectNode getIndexedDataDAOConfig(RequestContext requestContext) {
        throw new BadRequestException("Reading data from this indexer is not supported.");
    }

    public EntityDAO getEntityDAO(RequestContext requestContext) {
        return EntityDAOUtilities.getEntityDAO(daoConfigs, requestContext,
                getIndexedDataDAOConfig(requestContext), injector);
    }
}
