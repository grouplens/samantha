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

import org.grouplens.samantha.modeler.boosting.AbstractGBCent;
import org.grouplens.samantha.modeler.boosting.GBCentLearningInstance;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.common.PredictiveModel;
import org.grouplens.samantha.modeler.featurizer.Feature;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.model.IndexSpace;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;

import java.util.List;

public class XGBoostGBCent extends AbstractGBCent implements PredictiveModel {

    public XGBoostGBCent(List<FeatureExtractor> treeExtractors,
                         List<String> treeFeatures, String labelName, String weightName,
                         IndexSpace indexSpace, SVDFeature svdFeature) {
        super(indexSpace, treeExtractors, treeFeatures, labelName, weightName, null, svdFeature);
    }

    public double[] predict(LearningInstance ins) {
        GBCentLearningInstance centIns = (GBCentLearningInstance) ins;
        double pred = svdfeaModel.predict(centIns.getSvdfeaIns())[0];
        for (Feature feature : centIns.getSvdfeaIns().getBiasFeatures()) {
            int idx = feature.getIndex();
            if (idx < trees.size()) {
                PredictiveModel tree = trees.get(idx);
                if (tree != null) {
                    pred += tree.predict(new XGBoostInstance(centIns.getTreeIns()))[0];
                }
            }
        }
        double[] preds = new double[1];
        preds[0] = pred;
        return preds;
    }

    public PredictiveModel getNumericalTree(int treeIdx) {
        return new XGBoostModel(indexSpace, featureExtractors, features, labelName, weightName);
    }

    public void publishModel() {}
}
