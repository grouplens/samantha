package org.grouplens.samantha.server.dao;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.dao.CSVFileEntityDAO;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class CSVFileEntityDAOConfig implements EntityDAOConfig {
    final private String filePathKey;
    final private String separatorKey;

    private CSVFileEntityDAOConfig(String filePathKey, String separatorKey) {
        this.filePathKey = filePathKey;
        this.separatorKey = separatorKey;
    }

    public static EntityDAOConfig getEntityDAOConfig(Configuration daoConfig,
                                                     Injector injector) {
        return new CSVFileEntityDAOConfig(daoConfig.getString("filePathKey"),
                daoConfig.getString("separatorKey"));
    }

    public EntityDAO getEntityDAO(RequestContext requestContext, JsonNode daoConfig) {
        return new CSVFileEntityDAO(JsonHelpers.getRequiredString(daoConfig, separatorKey),
                JsonHelpers.getRequiredString(daoConfig, filePathKey));
    }
}
