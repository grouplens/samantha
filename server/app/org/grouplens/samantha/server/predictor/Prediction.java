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

package org.grouplens.samantha.server.predictor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.common.LearningInstance;
import play.libs.Json;

public class Prediction {
    final private LearningInstance instance;

    final private ObjectNode entity;

    final private double score;

    final private double[] scores;

    public Prediction(ObjectNode entity, LearningInstance ins, double score, double[] scores) {
        this.entity = entity;
        this.instance = ins;
        this.score = score;
        this.scores = scores;
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

    public double[] getScores() {
        return scores;
    }

    public JsonNode toJson() {
        ObjectNode obj = Json.newObject();
        obj.put("score", score);
        obj.set("attributes", entity);
        if (scores != null) {
            ArrayNode scoreArr = Json.newArray();
            for (int i = 0; i < scores.length; i++) {
                scoreArr.add(scores[i]);
            }
            obj.set("scores", scoreArr);
        }
        if (instance != null) {
            obj.set("instance", Json.toJson(instance));
        }
        return obj;
    }
}
