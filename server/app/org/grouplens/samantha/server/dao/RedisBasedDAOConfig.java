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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRRedisS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIRedis OF MERCHANTABILITY,
 * FITNRedisS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGRedis OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.grouplens.samantha.server.dao;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.retriever.RedisKeyBasedRetriever;
import org.grouplens.samantha.server.retriever.Retriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import play.inject.Injector;

public class RedisBasedDAOConfig implements EntityDAOConfig {
    private static Logger logger = LoggerFactory.getLogger(RedisBasedDAOConfig.class);
    private final Injector injector;
    private final String retrieverName;

    private RedisBasedDAOConfig(Injector injector,
                             String retrieverName) {
        this.injector = injector;
        this.retrieverName = retrieverName;
    }

    static public EntityDAOConfig getEntityDAOConfig(Configuration daoConfig,
                                                     Injector injector) {
        return new RedisBasedDAOConfig(injector, daoConfig.getString("retriever"));
    }

    public EntityDAO getEntityDAO(RequestContext requestContext, JsonNode daoConfig) {
        SamanthaConfigService configService = injector
                .instanceOf(SamanthaConfigService.class);
        Retriever retriever = configService.getRetriever(retrieverName, requestContext);
        if (!(retriever instanceof RedisKeyBasedRetriever)) {
            logger.warn("Retriever {} is not a {}", retrieverName, RedisKeyBasedRetriever.class);
        }
        return new RetrieverBasedDAO(configService.getRetriever(retrieverName, requestContext), requestContext);
    }
}
