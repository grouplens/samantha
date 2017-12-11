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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import org.grouplens.samantha.server.retriever.Retriever;

import java.util.List;

public class RetrieverBasedDAO implements EntityDAO {
    private final SamanthaConfigService configService;
    private final String retrieverName;
    private final RequestContext requestContext;
    private Retriever retriever;
    private int idx = 0;
    private List<ObjectNode> entityList = null;

    public RetrieverBasedDAO(String retrieverName, SamanthaConfigService configService,
                             RequestContext requestContext) {
        this.retrieverName = retrieverName;
        this.configService = configService;
        this.retriever = configService.getRetriever(retrieverName, requestContext);
        this.requestContext = requestContext;
    }

    public ObjectNode getNextEntity() {
        if (entityList != null && idx < entityList.size()) {
            return entityList.get(idx++);
        } else {
            RetrievedResult retrievedResult = retriever.retrieve(requestContext);
            entityList = retrievedResult.getEntityList();
            idx = 0;
            if (idx < entityList.size()) {
                return entityList.get(idx);
            } else {
                return null;
            }
        }
    }

    public boolean hasNextEntity() {
        if (entityList == null || idx >= entityList.size()) {
            RetrievedResult retrievedResult = retriever.retrieve(requestContext);
            entityList = retrievedResult.getEntityList();
            idx = 0;
        }
        if (idx < entityList.size()) {
            return true;
        } else {
            return false;
        }
    }

    public void restart() {
        retriever = configService.getRetriever(retrieverName, requestContext);
        idx = 0;
        entityList = null;
    }

    public void close() {}
}
