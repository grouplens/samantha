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
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;

import java.util.ArrayList;
import java.util.List;

public class SyncFeaturizedLearningData implements LearningData {
    private final GroupedEntityList groupedEntityList;
    private final EntityDAO entityDAO;
    private final Featurizer featurizer;
    private final boolean update;

    public SyncFeaturizedLearningData(EntityDAO entityDAO,
                                      List<String> groupKeys,
                                      Featurizer featurizer,
                                      boolean update) {
        this.entityDAO = entityDAO;
        this.featurizer = featurizer;
        this.update = update;
        if (groupKeys != null && groupKeys.size() > 0) {
            this.groupedEntityList = new GroupedEntityList(groupKeys, entityDAO);
        } else {
            this.groupedEntityList = null;
        }
    }


    public List<LearningInstance> getLearningInstance() {
        List<LearningInstance> instances;
        if (groupedEntityList != null) {
            List<ObjectNode> entityList;
            synchronized (groupedEntityList) {
                entityList = groupedEntityList.getNextGroup();
            }
            instances = FeaturizerUtilities.featurize(entityList, featurizer, update);
            entityList.clear();
            return instances;
        } else {
            ObjectNode cur = null;
            synchronized (entityDAO) {
                if (entityDAO.hasNextEntity()) {
                    cur = entityDAO.getNextEntity();
                } else {
                    entityDAO.close();
                }
            }
            instances = new ArrayList<>(1);
            if (cur != null) {
                instances.add(featurizer.featurize(cur, update));
            }
            return instances;
        }
    }

    synchronized public void startNewIteration() {
        if (groupedEntityList != null) {
            groupedEntityList.restart();
        } else {
            entityDAO.restart();
        }
    }
}
