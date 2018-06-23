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

package org.grouplens.samantha.modeler.xgboost;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningMethod;
import org.grouplens.samantha.modeler.common.PredictiveModel;
import org.grouplens.samantha.server.exception.BadRequestException;

import java.util.HashMap;
import java.util.Map;

public class XGBoostMethod implements LearningMethod {
    final private Map<String, Object> params;
    final private int round;

    public XGBoostMethod(Map<String, Object> params, int round) {
        this.params = params;
        this.round = round;
    }

    public void learn(PredictiveModel model, LearningData learningData, LearningData validData) {
        try {
            DMatrix dtrain = new DMatrix(new XGBoostIterator(learningData), null);
            Map<String, DMatrix> watches = new HashMap<>();
            if (validData != null) {
                watches.put("Validation", new DMatrix(new XGBoostIterator(validData), null));
            }
            Booster booster = XGBoost.train(dtrain, params, round, watches, null, null);
            XGBoostModel boostModel = (XGBoostModel) model;
            boostModel.setXGBooster(booster);
        } catch (XGBoostError e) {
            throw new BadRequestException(e);
        }
    }
}
