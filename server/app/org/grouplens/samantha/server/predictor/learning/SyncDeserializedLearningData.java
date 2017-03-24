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

package org.grouplens.samantha.server.predictor.learning;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.featurizer.GroupedEntityList;
import org.grouplens.samantha.server.exception.BadRequestException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class SyncDeserializedLearningData implements LearningData {
    private final EntityDAO entityDAO;
    private final GroupedEntityList groupedEntityList;
    private final String insAttr;
    private final String labelAttr;
    private final String weightAttr;

    //TODO: since learning instance now has group info, groupKeys is not necessary anymore
    public SyncDeserializedLearningData(EntityDAO entityDAO, String insAttr, List<String> groupKeys,
                                        String labelAttr, String weightAttr) {
        this.entityDAO = entityDAO;
        this.insAttr = insAttr;
        this.labelAttr = labelAttr;
        this.weightAttr = weightAttr;
        if (groupKeys != null && groupKeys.size() > 0) {
            groupedEntityList = new GroupedEntityList(groupKeys, entityDAO);
        } else {
            groupedEntityList = null;
        }
    }

    private LearningInstance deserializeLearningInstance(ObjectNode entity) {
        try {
            byte[] data = Base64.getDecoder().decode(entity.get(insAttr).asText());
            ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(data));
            LearningInstance instance = (LearningInstance) stream.readUnshared();
            stream.close();
            if (entity.has(labelAttr)) {
                instance.setLabel(entity.get(labelAttr).asDouble());
            }
            if (entity.has(weightAttr)) {
                instance.setWeight(entity.get(weightAttr).asDouble());
            }
            return instance;
        } catch (IOException | ClassNotFoundException e) {
            throw new BadRequestException(e);
        }
    }

    private List<LearningInstance> deserializeEntityList(List<ObjectNode> entityList) {
        List<LearningInstance> instances = new ArrayList<>(entityList.size());
        for (ObjectNode entity : entityList) {
            instances.add(deserializeLearningInstance(entity));
        }
        return instances;
    }

    public List<LearningInstance> getLearningInstance() {
        List<LearningInstance> instances;
        if (groupedEntityList == null) {
            LearningInstance instance;
            ObjectNode entity;
            synchronized (entityDAO) {
                if (entityDAO.hasNextEntity()) {
                    entity = entityDAO.getNextEntity();
                } else {
                    entityDAO.close();
                    return null;
                }
            }
            instance = deserializeLearningInstance(entity);
            instances = new ArrayList<>(1);
            instances.add(instance);
            return instances;
        } else {
            List<ObjectNode> entityList;
            synchronized (groupedEntityList) {
                entityList = groupedEntityList.getNextGroup();
            }
            instances = deserializeEntityList(entityList);
            entityList.clear();
            return instances;
        }
    }

    synchronized public void startNewIteration() {
        if (groupedEntityList == null) {
            entityDAO.restart();
        } else {
            groupedEntityList.restart();
        }
    }
}
