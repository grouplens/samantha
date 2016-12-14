package org.grouplens.samantha.server.dao;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.dao.CSVDirDAO;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class CSVDirDAOConfig implements EntityDAOConfig {
    final private String dirPathKey;
    final private String separatorKey;

    private CSVDirDAOConfig(String dirPathKey, String separatorKey) {
        this.dirPathKey = dirPathKey;
        this.separatorKey = separatorKey;
    }

    public static EntityDAOConfig getEntityDAOConfig(Configuration daoConfig,
                                                     Injector injector) {
        return new CSVDirDAOConfig(daoConfig.getString("dirPathKey"),
                daoConfig.getString("separatorKey"));
    }

    public EntityDAO getEntityDAO(RequestContext requestContext, JsonNode daoConfig) {
        return new CSVDirDAO(JsonHelpers.getRequiredString(daoConfig, dirPathKey),
                JsonHelpers.getRequiredString(daoConfig, separatorKey));
    }
}
