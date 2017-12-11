package org.grouplens.samantha.server.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class NullIndexer extends AbstractIndexer {

    public NullIndexer(Configuration config,
                       SamanthaConfigService configService,
                       Injector injector, String daoConfigKey,
                       Configuration daoConfigs) {
        super(config, configService, daoConfigs, daoConfigKey, injector);
    }

    public void index(JsonNode data, RequestContext requestContext) {}
}
