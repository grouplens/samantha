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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.common.DataOperation;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class JsonFileIndexer extends AbstractIndexer {
    private final FileWriterService writerService;
    private final String type;
    private final String tstampField;

    public JsonFileIndexer(Configuration config, SamanthaConfigService configService, Configuration daoConfigs,
                           String daoConfigKey, Injector injector, FileWriterService writerService,
                           String type, String tstampField) {
        super(config, configService, daoConfigs, daoConfigKey, injector);
        this.writerService = writerService;
        this.tstampField = tstampField;
        this.type = type;
    }

    public void index(JsonNode documents, RequestContext requestContext) {
        JsonNode reqBody = requestContext.getRequestBody();
        String operation = JsonHelpers.getOptionalString(reqBody, ConfigKey.DATA_OPERATION.get(),
                DataOperation.INSERT.get());
        if (operation.equals(DataOperation.INSERT.get()) || operation.equals(DataOperation.UPSERT.get())) {
            if (!documents.isArray()) {
                writerService.writeJson(type, documents, documents.get(tstampField).asInt());
            } else {
                for (JsonNode document : documents) {
                    writerService.writeJson(type, document, document.get(tstampField).asInt());
                }
            }
        } else {
            throw new BadRequestException("Data operation " + operation + " is not supported");
        }
    }

    //TODO: implementation
    public ObjectNode getIndexedDataDAOConfig(RequestContext requestContext) {
        throw new BadRequestException("Reading data from this indexer is not supported yet.");
    }
}
