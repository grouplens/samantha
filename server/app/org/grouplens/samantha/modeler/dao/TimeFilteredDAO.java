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

package org.grouplens.samantha.modeler.dao;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;

public class TimeFilteredDAO implements EntityDAO {
    private final int beginTime;
    private final int endTime;
    private final String timestampField;
    private final EntityDAO entityDAO;
    private ObjectNode cur;

    public TimeFilteredDAO(EntityDAO entityDAO, int beginTime, int endTime,
                           String timestampField) {
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.entityDAO = entityDAO;
        this.timestampField = timestampField;
    }

    public boolean hasNextEntity() {
        if (cur != null) {
            return true;
        }
        while (entityDAO.hasNextEntity()) {
            ObjectNode entity = entityDAO.getNextEntity();
            int tstamp = entity.get(timestampField).asInt();
            if (tstamp >= beginTime && tstamp <= endTime) {
                cur = entity;
                return true;
            }
        }
        return false;
    }

    public ObjectNode getNextEntity() {
        ObjectNode ret;
        if (cur != null) {
            ret = cur;
            cur = null;
            return ret;
        } else {
            if (hasNextEntity()) {
                return cur;
            } else {
                return null;
            }
        }
    }

    public void restart() {
        entityDAO.restart();
        cur = null;
    }

    public void close() {
        entityDAO.close();
        cur = null;
    }
}
