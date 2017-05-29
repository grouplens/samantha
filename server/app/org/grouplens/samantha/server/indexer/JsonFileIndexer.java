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
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

//TODO: needs to refactor to use file service because the writer opened is never closed, temporary solution
public class JsonFileIndexer extends AbstractIndexer {
    private final BufferedWriter writer;

    public JsonFileIndexer(Configuration config, SamanthaConfigService configService, Configuration daoConfigs,
                           String daoConfigKey, Injector injector, String filePath) {
        super(config, configService, daoConfigs, daoConfigKey, injector);
        try {
            this.writer = new BufferedWriter(new FileWriter(filePath));
        } catch (IOException e) {
            throw new BadRequestException(e);
        }
    }

    public void index(JsonNode documents, RequestContext requestContext) {
        try {
            writer.write(documents.toString() + '\n');
        } catch (IOException e) {
            throw new BadRequestException(e);
        }
    }

    public ObjectNode getIndexedDataDAOConfig(RequestContext requestContext) {
        try {
            writer.close();
        } catch (IOException e) {
            throw new BadRequestException(e);
        }
        throw new BadRequestException("Indexer successfully closed, " +
                "but reading data from this indexer is not supported.");
    }
}
