package org.grouplens.samantha.server.predictor.learning;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.exception.InvalidRequestException;
import play.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Base64;

public class SyncDeserializedLearningData implements LearningData {
    private final EntityDAO entityDAO;
    private final String insAttr;
    private final String labelAttr;
    private final String weightAttr;

    public SyncDeserializedLearningData(EntityDAO entityDAO, String insAttr,
                                        String labelAttr, String weightAttr) {
        this.entityDAO = entityDAO;
        this.insAttr = insAttr;
        this.labelAttr = labelAttr;
        this.weightAttr = weightAttr;
    }

    private LearningInstance deserializeLearningInstance(String insStr) {
        byte[] data = Base64.getDecoder().decode(insStr);
        try {
            ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(data));
            LearningInstance instance = (LearningInstance) stream.readUnshared();
            stream.close();
            return instance;
        } catch (IOException | ClassNotFoundException e) {
            Logger.error(e.getMessage());
            throw new InvalidRequestException(e);
        }
    }

    public LearningInstance getLearningInstance() {
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
        instance = deserializeLearningInstance(entity.get(insAttr).asText());
        if (entity.has(labelAttr)) {
            instance.setLabel(entity.get(labelAttr).asDouble());
        }
        if (entity.has(weightAttr)) {
            instance.setWeight(entity.get(weightAttr).asDouble());
        }
        return instance;
    }

    synchronized public void startNewIteration() {
        entityDAO.restart();
    }
}
