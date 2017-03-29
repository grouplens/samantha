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
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.retriever.ESQueryBasedRetriever;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import play.Configuration;
import play.inject.Injector;

public class ESBasedDAOConfig implements EntityDAOConfig {
    final Injector injector;
    final String retrieverName;
    final String elasticSearchReqKey;

    private ESBasedDAOConfig(Injector injector,
                             String retrieverName,
                             String elasticSearchReqKey) {
        this.injector = injector;
        this.retrieverName = retrieverName;
        this.elasticSearchReqKey = elasticSearchReqKey;
    }

    static public EntityDAOConfig getEntityDAOConfig(Configuration daoConfig,
                                              Injector injector) {
        return new ESBasedDAOConfig(injector,
                daoConfig.getString("retriever"), daoConfig.getString("elasticSearchReqKey"));
    }

    public EntityDAO getEntityDAO(RequestContext requestContext, JsonNode daoConfig) {
        SamanthaConfigService configService = injector
                .instanceOf(SamanthaConfigService.class);
        return new ESBasedDAO((ESQueryBasedRetriever)configService
                .getRetriever(retrieverName, requestContext), requestContext, this);
    }
}
