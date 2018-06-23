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

package org.grouplens.samantha.modeler.svdfeature;

import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;

import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.modeler.model.IndexSpace;
import org.grouplens.samantha.modeler.model.SpaceMode;
import org.grouplens.samantha.modeler.model.SpaceProducer;
import org.grouplens.samantha.modeler.model.VariableSpace;

import javax.inject.Inject;
import java.util.List;

public class SVDFeatureProducer {
    @Inject private SpaceProducer spaceProducer;

    @Inject
    private SVDFeatureProducer() {}

    public SVDFeatureProducer(SpaceProducer spaceProducer) {
        this.spaceProducer = spaceProducer;
    }

    private IndexSpace getIndexSpace(String spaceName, SpaceMode spaceMode) {
        IndexSpace indexSpace = spaceProducer.getIndexSpace(spaceName, spaceMode);
        indexSpace.requestKeyMap(SVDFeatureKey.BIASES.get());
        indexSpace.requestKeyMap(SVDFeatureKey.FACTORS.get());
        return indexSpace;
    }

    private VariableSpace getVariableSpace(String spaceName, SpaceMode spaceMode,
                                           int biasSize, int factSize, int factDim) {
        VariableSpace variableSpace = spaceProducer.getVariableSpace(spaceName, spaceMode);
        variableSpace.requestScalarVar(SVDFeatureKey.BIASES.get(), biasSize, 0.0, false);
        variableSpace.requestScalarVar(SVDFeatureKey.SUPPORT.get(), biasSize, 0.0, false);
        variableSpace.requestVectorVar(SVDFeatureKey.FACTORS.get(), factSize, factDim, 0.0, true, false);
        return variableSpace;
    }

    public SVDFeature createSVDFeatureModel(String modelName,
                                            SpaceMode spaceMode,
                                            List<String> biasFeas,
                                            List<String> ufactFeas,
                                            List<String> ifactFeas,
                                            String labelName,
                                            String weightName,
                                            List<String> groupKeys,
                                            List<FeatureExtractor> featureExtractors,
                                            int factDim,
                                            ObjectiveFunction objectiveFunction) {
        IndexSpace indexSpace = getIndexSpace(modelName, spaceMode);
        VariableSpace variableSpace = getVariableSpace(modelName, spaceMode, 0, 0, factDim);
        return new SVDFeature(biasFeas, ufactFeas, ifactFeas, labelName, weightName, groupKeys,
                featureExtractors, factDim, objectiveFunction, indexSpace, variableSpace);
    }
}
