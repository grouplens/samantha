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

package org.grouplens.samantha.server.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.util.List;

public class CatIndexer extends AbstractIndexer {
    private static Logger logger = LoggerFactory.getLogger(CatIndexer.class);
    private final String indexersDaoConfigKey;
    private final String daoNameKey;
    private final String daoName;
    private final List<String> indexerNames;

    public CatIndexer(SamanthaConfigService configService,
                      Configuration config, Injector injector,
                      Configuration daoConfigs,
                      String daoConfigKey, String indexersDaoConfigKey,
                      List<String> indexerNames, String daoName,
                      String daoNameKey, int batchSize, RequestContext requestContext) {
        super(config, configService, daoConfigs, daoConfigKey, batchSize, requestContext, injector);
        this.indexersDaoConfigKey = indexersDaoConfigKey;
        this.indexerNames = indexerNames;
        this.daoName = daoName;
        this.daoNameKey = daoNameKey;
    }

    public void index(JsonNode data, RequestContext requestContext) {
        logger.error("This indexer does not support indexing data. It only supports scanning data.");
    }

    public ObjectNode getIndexedDataDAOConfig(RequestContext requestContext) {
        ObjectNode ret = Json.newObject();
        ArrayNode daos = Json.newArray();
        ret.set(indexersDaoConfigKey, daos);
        for (String indexerName: indexerNames) {
            Indexer indexer = configService.getIndexer(indexerName, requestContext);
            daos.add(indexer.getIndexedDataDAOConfig(requestContext));
        }
        ret.put(daoNameKey, daoName);
        return ret;
    }
}
