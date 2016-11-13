package org.grouplens.samantha.xgboost;

import ml.dmlc.xgboost4j.LabeledPoint;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;

import java.util.Iterator;

public class XGBoostIterator implements Iterator {
    final private LearningData data;
    private XGBoostInstance instance;

    XGBoostIterator(LearningData data) {
        this.data = data;
    }

    @Override
    public LabeledPoint next() {
        if (instance != null) {
            XGBoostInstance back = instance;
            instance = null;
            return back.getLabeledPoint();
        } else {
            return ((XGBoostInstance)data.getLearningInstance()).getLabeledPoint();
        }
    }

    @Override
    public boolean hasNext() {
        if (instance != null) {
            return true;
        }
        LearningInstance ins = data.getLearningInstance();
        if (ins == null) {
            return false;
        } else {
            instance = (XGBoostInstance)ins;
            return true;
        }
    }
}
