package org.grouplens.samantha.server.retriever;

import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class RequestBasedRetrieverConfig implements RetrieverConfig {
    private final Configuration entityDaoConfigs;
    private final Injector injector;
    private final List<Configuration> expandersConfig;
    private final String daoConfigKey;

    private RequestBasedRetrieverConfig(Configuration entityDaoConfigs,
                                        List<Configuration> expandersConfig,
                                        Injector injector, String daoConfigKey) {
        this.entityDaoConfigs = entityDaoConfigs;
        this.expandersConfig = expandersConfig;
        this.injector = injector;
        this.daoConfigKey = daoConfigKey;
    }

    public static RetrieverConfig getRetrieverConfig(Configuration retrieverConfig,
                                                     Injector injector) {
        List<Configuration> expanders = ExpanderUtilities.getEntityExpandersConfig(retrieverConfig);
        return new RequestBasedRetrieverConfig(retrieverConfig
                .getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get()),
                expanders,
                injector, retrieverConfig.getString("daoConfigKey"));
    }

    public Retriever getRetriever(RequestContext requestContext) {
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        return new RequestBasedRetriever(entityDaoConfigs, entityExpanders, injector, daoConfigKey);
    }
}
