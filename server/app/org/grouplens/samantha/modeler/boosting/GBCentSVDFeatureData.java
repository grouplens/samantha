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

package org.grouplens.samantha.modeler.boosting;

import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;

import java.util.ArrayList;
import java.util.List;

public class GBCentSVDFeatureData implements LearningData {

    private final LearningData learningData;

    GBCentSVDFeatureData(LearningData learningData) {
        this.learningData = learningData;
    }

    public void startNewIteration() {
        learningData.startNewIteration();
    }

    public List<LearningInstance> getLearningInstance() {
        List<LearningInstance> instances = learningData.getLearningInstance();
        if (instances.size() == 0) {
            return instances;
        }
        List<LearningInstance> curList = new ArrayList<>(instances.size());
        for (LearningInstance ins : instances) {
            GBCentLearningInstance centIns = (GBCentLearningInstance) ins;
            curList.add(centIns.getSvdfeaIns());
        }
        return curList;
    }
}
