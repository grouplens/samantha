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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.io.IOUtilities;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

public class RequestEntityDAO implements EntityDAO {
    final private List<ObjectNode> entityList;
    private int idx = 0;

    public RequestEntityDAO(ArrayNode entities) {
        entityList = new ArrayList<>(entities.size());
        for (JsonNode json : entities) {
            ObjectNode entity = Json.newObject();
            IOUtilities.parseEntityFromJsonNode(json, entity);
            entityList.add(entity);
        }
    }

    public boolean hasNextEntity() {
        return idx < entityList.size();
    }

    public ObjectNode getNextEntity() {
        return entityList.get(idx++);
    }

    public void restart() {
        idx = 0;
    }

    public void close() {}
}
