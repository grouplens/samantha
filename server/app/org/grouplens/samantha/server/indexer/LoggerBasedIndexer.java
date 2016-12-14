package org.grouplens.samantha.server.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.Logger;
import play.inject.Injector;

public class LoggerBasedIndexer extends AbstractIndexer {

    public LoggerBasedIndexer(Configuration config, SamanthaConfigService configService,
                              String daoConfigKey, Configuration daoConfigs, Injector injector) {
        super(config, configService, daoConfigs, daoConfigKey, injector);
    }

    public void index(JsonNode documents, RequestContext requestContext) {
        Logger.info(documents.toString());
    }

    public void index(RequestContext requestContext) {
        Logger.info(requestContext.getRequestBody().toString());
    }

    public ObjectNode getIndexedDataDAOConfig(RequestContext requestContext) {
        throw new BadRequestException("Getting indexed data is not supported in LoggerBasedIndexer.");
    }
}
