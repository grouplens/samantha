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
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;

import java.util.ArrayList;
import java.util.List;

public class ExpandedEntityDAO implements EntityDAO {
    private final List<EntityExpander> expanders;
    private final EntityDAO entityDAO;
    private final RequestContext requestContext;
    private List<ObjectNode> entityList = new ArrayList<>();
    private int idx = 0;

    public ExpandedEntityDAO(List<EntityExpander> expanders, EntityDAO entityDAO, RequestContext requestContext) {
        this.expanders = expanders;
        this.entityDAO = entityDAO;
        this.requestContext = requestContext;
    }

    public boolean hasNextEntity() {
        if (idx >= entityList.size()) {
            idx = 0;
            entityList = ExpanderUtilities.expandFromEntityDAO(entityDAO, entityList, expanders, requestContext);
            if (idx < entityList.size()) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public ObjectNode getNextEntity() {
        if (idx < entityList.size()) {
            return entityList.get(idx++);
        } else {
            idx = 0;
            entityList = ExpanderUtilities.expandFromEntityDAO(entityDAO, entityList, expanders, requestContext);
            if (entityList.size() > 0) {
                return entityList.get(idx++);
            }
        }
        return null;
    }

    public void restart() {
        entityDAO.restart();
        entityList.clear();
        idx = 0;
    }

    public void close() {
        entityDAO.close();
        entityList.clear();
        idx = 0;
    }
}
