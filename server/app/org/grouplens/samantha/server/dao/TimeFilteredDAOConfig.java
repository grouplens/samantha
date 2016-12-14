package org.grouplens.samantha.server.dao;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.indexer.IndexerUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class TimeFilteredDAOConfig implements EntityDAOConfig {
    final private Configuration daosConfig;
    final private Injector injector;
    final private String beginTime;
    final private String endTime;
    final private String beginTimeKey;
    final private String endTimeKey;
    final private String timestampField;
    final private String subDaoConfigKey;

    private TimeFilteredDAOConfig(Configuration daosConfig, Injector injector,
                                  String beginTime, String endTime, String timestampField,
                                  String beginTimeKey, String endTimeKey, String subDaoConfigKey) {
        this.daosConfig = daosConfig;
        this.injector = injector;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.beginTimeKey = beginTimeKey;
        this.endTimeKey = endTimeKey;
        this.subDaoConfigKey = subDaoConfigKey;
        this.timestampField = timestampField;
    }

    public static EntityDAOConfig getEntityDAOConfig(Configuration daoConfig,
                                                     Injector injector) {
        return new TimeFilteredDAOConfig(daoConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get()),
                injector, daoConfig.getString("beginTime"),
                daoConfig.getString("endTime"),
                daoConfig.getString("timestampField"),
                daoConfig.getString("beginTimeKey"),
                daoConfig.getString("endTimeKey"),
                daoConfig.getString("subDaoConfigKey"));
    }

    public EntityDAO getEntityDAO(RequestContext requestContext, JsonNode daoConfig) {
        JsonNode subDaoConfig = JsonHelpers.getRequiredJson(daoConfig, subDaoConfigKey);
        EntityDAO entityDAO = EntityDAOUtilities.getEntityDAO(daosConfig,
                requestContext, subDaoConfig, injector);
        String startStr = JsonHelpers.getOptionalString(daoConfig, beginTimeKey,
                beginTime);
        String endStr = JsonHelpers.getOptionalString(daoConfig, endTimeKey,
                endTime);
        int start = IndexerUtilities.parseTime(startStr);
        int end = IndexerUtilities.parseTime(endStr);
        return new TimeFilteredDAO(entityDAO, start, end, timestampField);
    }
}
