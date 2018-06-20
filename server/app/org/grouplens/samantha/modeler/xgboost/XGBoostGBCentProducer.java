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

package org.grouplens.samantha.modeler.xgboost;

import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.modeler.model.IndexSpace;
import org.grouplens.samantha.modeler.model.SpaceMode;
import org.grouplens.samantha.modeler.model.SpaceProducer;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureProducer;
import org.grouplens.samantha.modeler.tree.TreeKey;

import javax.inject.Inject;
import java.util.List;

public class XGBoostGBCentProducer {
    @Inject
    private SpaceProducer spaceProducer;

    @Inject
    private XGBoostGBCentProducer() {}

    public XGBoostGBCentProducer(SpaceProducer spaceProducer) {
        this.spaceProducer = spaceProducer;
    }

    public XGBoostGBCent createGBCent(String modelName, SpaceMode spaceMode,
                                      String labelName, String weightName,
                                      List<FeatureExtractor> svdfeaExtractors,
                                      List<FeatureExtractor> treeExtractors,
                                      List<String> biasFeas, List<String> ufactFeas,
                                      List<String> ifactFeas, List<String> treeFeas,
                                      int factDim, ObjectiveFunction objectiveFunction) {
        IndexSpace indexSpace = spaceProducer.getIndexSpace(modelName, spaceMode);
        indexSpace.requestKeyMap(TreeKey.TREE.get());
        SVDFeatureProducer svdfeaProducer = new SVDFeatureProducer(spaceProducer);
        SVDFeature svdfeaModel = svdfeaProducer.createSVDFeatureModel(modelName, spaceMode,
                biasFeas, ufactFeas, ifactFeas, labelName, weightName,
                null, svdfeaExtractors, factDim, objectiveFunction);
        return new XGBoostGBCent(treeExtractors, treeFeas, labelName, weightName,
                indexSpace, svdfeaModel);
    }

    public XGBoostGBCent createGBCentWithSVDFeatureModel(String modelName, SpaceMode spaceMode,
                                                         List<String> treeFeas,
                                                         List<FeatureExtractor> treeExtractors,
                                                         SVDFeature svdfeaModel) {
        IndexSpace indexSpace = spaceProducer.getIndexSpace(modelName, spaceMode);
        indexSpace.requestKeyMap(TreeKey.TREE.get());
        String labelName = svdfeaModel.getLabelName();
        String weightName = svdfeaModel.getWeightName();
        return new XGBoostGBCent(treeExtractors, treeFeas, labelName, weightName,
                indexSpace, svdfeaModel);
    }
}
