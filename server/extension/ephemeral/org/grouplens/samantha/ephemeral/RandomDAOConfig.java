package org.grouplens.samantha.ephemeral;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.dao.EntityDAOConfig;
import org.grouplens.samantha.server.dao.EntityDAOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class RandomDAOConfig implements EntityDAOConfig {
    final private Configuration entityDaoConfigs;
    final private Injector injector;
    final private String subDaoConfigKey;

    private RandomDAOConfig(Configuration entityDaoConfigs, Injector injector,
                            String subDaoConfigKey) {
        this.entityDaoConfigs = entityDaoConfigs;
        this.injector = injector;
        this.subDaoConfigKey = subDaoConfigKey;
    }

    public static EntityDAOConfig getEntityDAOConfig(Configuration daoConfig,
                                                     Injector injector) {
        return new RandomDAOConfig(daoConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get()),
                injector, daoConfig.getString("subDaoConfigKey"));
    }

    public EntityDAO getEntityDAO(RequestContext requestContext, JsonNode reqDao) {
        JsonNode subReqDao = JsonHelpers.getRequiredJson(reqDao, subDaoConfigKey);
        EntityDAO entityDAO = EntityDAOUtilities.getEntityDAO(entityDaoConfigs,
                requestContext, subReqDao, injector);
        return new RandomDAO(entityDAO);
    }
}
