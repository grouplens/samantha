package org.grouplens.samantha.server.dao;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.dao.CSVFileListDAO;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class CSVFileListDAOConfig implements EntityDAOConfig {
    final private String filesKey;
    final private String separatorKey;

    private CSVFileListDAOConfig(String filesKey, String separatorKey) {
        this.separatorKey = separatorKey;
        this.filesKey = filesKey;
    }

    public static EntityDAOConfig getEntityDAOConfig(Configuration daoConfig,
                                                     Injector injector) {
        return new CSVFileListDAOConfig(daoConfig.getString("filesKey"),
                daoConfig.getString("separatorKey"));
    }

    public EntityDAO getEntityDAO(RequestContext requestContext, JsonNode daoConfig) {
        return new CSVFileListDAO(JsonHelpers.getRequiredStringList(daoConfig, filesKey),
                JsonHelpers.getOptionalString(daoConfig, separatorKey, "\t"));
    }
}
