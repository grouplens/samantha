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

import ml.dmlc.xgboost4j.LabeledPoint;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

public class XGBoostIterator implements Iterator {
    private static Logger logger = LoggerFactory.getLogger(XGBoostIterator.class);
    final private LearningData data;
    private int iter = 0;
    private XGBoostInstance instance;
    private List<LearningInstance> instances;
    private int num = 0;

    XGBoostIterator(LearningData data) {
        this.data = data;
    }

    @Override
    public LabeledPoint next() {
        XGBoostInstance back = instance;
        instance = null;
        num++;
        return back.getLabeledPoint();
    }

    @Override
    public boolean hasNext() {
        if (instance != null) {
            return true;
        }
        if (instances == null) {
            instances = data.getLearningInstance();
            iter = 0;
        } else if (instances.size() <= iter) {
            instances = data.getLearningInstance();
            iter = 0;
        }
        if (iter >= instances.size()) {
            logger.info("Number of data points: {}", num);
            return false;
        } else {
            instance = (XGBoostInstance)instances.get(iter++);
            return true;
        }
    }
}
