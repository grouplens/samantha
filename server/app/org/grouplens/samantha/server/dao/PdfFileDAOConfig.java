package org.grouplens.samantha.server.dao;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.dao.PdfFileDAO;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class PdfFileDAOConfig implements EntityDAOConfig {
    final private String filePathKey;

    private PdfFileDAOConfig(String filePathKey) {
        this.filePathKey = filePathKey;
    }

    public static EntityDAOConfig getEntityDAOConfig(Configuration daoConfig,
                                                     Injector injector) {
        return new PdfFileDAOConfig(daoConfig.getString("filePathKey"));
    }

    public EntityDAO getEntityDAO(RequestContext requestContext, JsonNode daoConfig) {
        return new PdfFileDAO(JsonHelpers.getRequiredString(daoConfig, filePathKey));
    }
}
