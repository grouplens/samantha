package org.grouplens.samantha.server.dao;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class EntityDAOUtilities {

    private EntityDAOUtilities() {}

    static public EntityDAO getEntityDAO(Configuration entityDaoConfigs, RequestContext requestContext,
                                         JsonNode reqDao, Injector injector) {
        String entityDaoKey = entityDaoConfigs.getString("entityDaoKey");
        String entityDaoConfigName = JsonHelpers.getRequiredString(reqDao, entityDaoKey);
        Configuration entityDaoConfig = entityDaoConfigs.getConfig(entityDaoConfigName);
        String entityDaoConfigClass = entityDaoConfig.getString(ConfigKey.DAO_CONFIG_CLASS.get());
        try {
            Method method = Class.forName(entityDaoConfigClass)
                    .getMethod("getEntityDAOConfig", Configuration.class, Injector.class);
            EntityDAOConfig config =  (EntityDAOConfig) method
                    .invoke(null, entityDaoConfig, injector);
            return config.getEntityDAO(requestContext, reqDao);
        } catch (IllegalAccessException | InvocationTargetException
                | NoSuchMethodException | ClassNotFoundException e) {
            throw new BadRequestException(e);
        }
    }
}
