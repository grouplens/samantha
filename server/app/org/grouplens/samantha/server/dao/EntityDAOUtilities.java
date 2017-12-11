/*
 * Copyright (c) [2016-2017] [University of Minnesota]
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
        String entityDaoConfigName = JsonHelpers.getRequiredString(reqDao, ConfigKey.ENTITY_DAO_NAME_KEY.get());
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
