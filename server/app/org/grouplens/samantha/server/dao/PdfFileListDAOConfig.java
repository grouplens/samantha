package org.grouplens.samantha.server.dao;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.dao.PdfFileListDAO;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class PdfFileListDAOConfig implements EntityDAOConfig {
    final private String filesKey;

    private PdfFileListDAOConfig(String filesKey) {
        this.filesKey = filesKey;
    }

    public static EntityDAOConfig getEntityDAOConfig(Configuration daoConfig,
                                                     Injector injector) {
        return new PdfFileListDAOConfig(daoConfig.getString("filesKey"));
    }

    public EntityDAO getEntityDAO(RequestContext requestContext, JsonNode daoConfig) {
        return new PdfFileListDAO(JsonHelpers.getRequiredStringList(daoConfig, filesKey));
    }
}
