package org.grouplens.samantha.server.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.grouplens.samantha.modeler.dao.ItemIDListDAO;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class RequestItemIDListDAOConfig implements EntityDAOConfig {
    final private String itemListKey;
    final private String attrName;

    private RequestItemIDListDAOConfig(String attrName, String itemListKey) {
        this.itemListKey = itemListKey;
        this.attrName = attrName;
    }

    static public EntityDAOConfig getEntityDAOConfig(Configuration daoConfig,
                                                     Injector injector) {
        return new RequestItemIDListDAOConfig(daoConfig.getString("attrName"),
                daoConfig.getString("itemListKey"));
    }

    public ItemIDListDAO getEntityDAO(RequestContext requestContext, JsonNode reqDao) {
        ArrayNode itemList = JsonHelpers.getRequiredArray(reqDao, itemListKey);
        return new ItemIDListDAO(itemList, attrName);
    }
}
