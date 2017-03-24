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

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.common.PredictiveModel;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.featurizer.StandardListLearningData;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.modeler.svdfeature.SVDFeatureInstance;
import org.grouplens.samantha.modeler.featurizer.StandardFeaturizer;
import org.grouplens.samantha.modeler.featurizer.StandardLearningInstance;

import java.util.ArrayList;
import java.util.List;

abstract public class AbstractGBCent extends StandardFeaturizer implements GBCent {
    protected final SVDFeature svdfeaModel;
    protected final List<PredictiveModel> trees = new ArrayList<>();

    public AbstractGBCent(IndexSpace indexSpace, List<FeatureExtractor> featureExtractors,
                          List<String> features, String labelName, String weightName,
                          List<String> groupKeys, SVDFeature svdfeaModel) {
        super(indexSpace, featureExtractors, features, groupKeys, labelName, weightName);
        this.svdfeaModel = svdfeaModel;
    }

    public LearningInstance featurize(JsonNode entity, boolean update) {
        SVDFeatureInstance svdIns = (SVDFeatureInstance) svdfeaModel.featurize(entity, update);
        StandardLearningInstance treeIns = (StandardLearningInstance) super.featurize(entity, update);
        return new GBCentLearningInstance(svdIns, treeIns);
    }

    public LearningData getLearningData(List<StandardLearningInstance> treeInstances) {
        return new StandardListLearningData(treeInstances);
    }

    public SVDFeature getSVDFeatureModel() {
        return this.svdfeaModel;
    }

    public void setNumericalTree(int treeIdx, PredictiveModel tree) {
        while (trees.size() < treeIdx + 1) {
            trees.add(null);
        }
        trees.set(treeIdx, tree);
    }
}
