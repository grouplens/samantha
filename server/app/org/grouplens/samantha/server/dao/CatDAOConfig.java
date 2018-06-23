/*
 * Copyright (c) [2016-2018] [University of Minnesota]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.grouplens.samantha.server.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.grouplens.samantha.modeler.dao.CatDAO;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class CatDAOConfig implements EntityDAOConfig {
    final private Configuration daosConfig;
    final private Injector injector;
    final private String subDaosConfigKey;

    private CatDAOConfig(Configuration daosConfig, Injector injector,
                         String subDaosConfigKey) {
        this.daosConfig = daosConfig;
        this.injector = injector;
        this.subDaosConfigKey = subDaosConfigKey;
    }

    public static EntityDAOConfig getEntityDAOConfig(Configuration daoConfig,
                                                     Injector injector) {
        return new CatDAOConfig(daoConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get()),
                injector, daoConfig.getString("subDaosConfigKey"));
    }

    public EntityDAO getEntityDAO(RequestContext requestContext, JsonNode daoConfig) {
        ArrayNode subDaosConfig = JsonHelpers.getRequiredArray(daoConfig, subDaosConfigKey);
        List<EntityDAO> entityDAOList = new ArrayList<>();
        for (JsonNode subDaoConfig : subDaosConfig) {
            entityDAOList.add(EntityDAOUtilities.getEntityDAO(daosConfig,
                    requestContext, subDaoConfig, injector));
        }
        return new CatDAO(entityDAOList);
    }
}
