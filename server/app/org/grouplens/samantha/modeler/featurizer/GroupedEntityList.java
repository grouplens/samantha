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

package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;

import java.util.ArrayList;
import java.util.List;

public class GroupedEntityList {
    private ObjectNode prevEntity;
    private String prevGroup;
    private final List<String> groupKeys;
    private final EntityDAO entityDAO;

    public GroupedEntityList(List<String> groupKeys, EntityDAO entityDAO) {
        this.entityDAO = entityDAO;
        this.groupKeys = groupKeys;
    }

    public List<ObjectNode> getNextGroup() {
        List<ObjectNode> entityList = new ArrayList<>();
        if (prevEntity != null) {
            entityList.add(prevEntity);
            prevEntity = null;
        }
        while (entityDAO.hasNextEntity()) {
            ObjectNode entity = entityDAO.getNextEntity();
            String group = FeatureExtractorUtilities.composeConcatenatedKey(entity, groupKeys);
            if (prevGroup == null) {
                prevGroup = group;
            }
            if (!group.equals(prevGroup)) {
                prevGroup = group;
                prevEntity = entity;
                break;
            } else {
                entityList.add(entity);
            }
        }
        return entityList;
    }

    public void restart() {
        entityDAO.restart();
        prevEntity = null;
        prevGroup = null;
    }

    public void close() {
        entityDAO.close();
        prevEntity = null;
        prevGroup = null;
    }
}
