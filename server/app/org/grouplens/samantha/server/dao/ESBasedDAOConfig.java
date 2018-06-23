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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.exception.ConfigurationException;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.retriever.ESQueryBasedRetriever;
import org.grouplens.samantha.server.retriever.Retriever;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

public class ESBasedDAOConfig implements EntityDAOConfig {
    private final Injector injector;
    private final String retrieverName;
    private final String retrieverNameKey;
    private final String setScrollKey;

    private ESBasedDAOConfig(Injector injector,
                             String retrieverName, String setScrollKey, String retrieverNameKey) {
        this.injector = injector;
        this.retrieverName = retrieverName;
        this.setScrollKey = setScrollKey;
        this.retrieverNameKey = retrieverNameKey;
    }

    static public EntityDAOConfig getEntityDAOConfig(Configuration daoConfig,
                                              Injector injector) {
        return new ESBasedDAOConfig(injector,
                daoConfig.getString("retrieverName"),
                daoConfig.getString("setScrollKey"),
                daoConfig.getString("retrieverNameKey"));
    }

    public EntityDAO getEntityDAO(RequestContext requestContext, JsonNode daoConfig) {
        SamanthaConfigService configService = injector
                .instanceOf(SamanthaConfigService.class);
        ObjectNode req = Json.newObject();
        IOUtilities.parseEntityFromJsonNode(daoConfig, req);
        String retrieverName = JsonHelpers.getOptionalString(daoConfig, retrieverNameKey, this.retrieverName);
        req.put(setScrollKey, true);
        RequestContext pseudoReq = new RequestContext(req, requestContext.getEngineName());
        Retriever retriever = configService.getRetriever(retrieverName, pseudoReq);
        if (!(retriever instanceof ESQueryBasedRetriever)) {
            throw new ConfigurationException(retrieverName + " must be of type " + ESQueryBasedRetriever.class);
        }
        return new RetrieverBasedDAO(retrieverName, configService, pseudoReq);
    }
}
