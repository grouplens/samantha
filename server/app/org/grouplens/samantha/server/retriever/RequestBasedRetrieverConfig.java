package org.grouplens.samantha.server.retriever;

import org.grouplens.samantha.server.common.AbstractComponentConfig;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class RequestBasedRetrieverConfig extends AbstractComponentConfig implements RetrieverConfig {
    private final Configuration entityDaoConfigs;
    private final Injector injector;
    private final String daoConfigKey;

    private RequestBasedRetrieverConfig(Configuration entityDaoConfigs,
                                        Injector injector, String daoConfigKey, Configuration config) {
        super(config);
        this.entityDaoConfigs = entityDaoConfigs;
        this.injector = injector;
        this.daoConfigKey = daoConfigKey;
    }

    public static RetrieverConfig getRetrieverConfig(Configuration retrieverConfig,
                                                     Injector injector) {
        return new RequestBasedRetrieverConfig(retrieverConfig
                .getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get()),
                injector, retrieverConfig.getString("daoConfigKey"), retrieverConfig);
    }

    public Retriever getRetriever(RequestContext requestContext) {
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        return new RequestBasedRetriever(entityDaoConfigs, entityExpanders, injector, daoConfigKey, config);
    }
}
