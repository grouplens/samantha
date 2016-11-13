package org.grouplens.samantha.server.predictor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.common.LearningInstance;
import play.Logger;
import play.libs.Json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Base64;

public class Prediction {
    final private LearningInstance instance;

    final private ObjectNode entity;

    final private double score;

    //TODO: support classification
    //final private String class;
    //final private DoubleList probabilities;
    //may need a builder for construction

    public Prediction(ObjectNode entity, LearningInstance ins, double score) {
        this.entity = entity;
        this.instance = ins;
        this.score = score;
    }

    public LearningInstance getInstance() {
        return instance;
    }

    public ObjectNode getEntity() {
        return entity;
    }

    public double getScore() {
        return score;
    }

    public JsonNode toJson() {
        ObjectNode obj = Json.newObject();
        obj.put("score", score);
        obj.set("attributes", entity);
        obj.put("instance", getInstanceString());
        return obj;
    }

    public String getInstanceString() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeUnshared(instance);
            baos.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            Logger.error(e.getMessage());
        }
        return null;
    }
}
