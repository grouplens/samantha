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

package org.grouplens.samantha.modeler.tree;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.featurizer.Featurizer;
import org.grouplens.samantha.modeler.featurizer.StandardFeaturizer;
import org.grouplens.samantha.modeler.featurizer.StandardLearningInstance;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.VariableSpace;

import java.util.List;

abstract public class AbstractDecisionTree implements DecisionTree, Featurizer {
    private final Featurizer featurizer;
    protected final IndexSpace indexSpace;
    protected final VariableSpace variableSpace;
    protected final List<FeatureExtractor> featureExtractors;
    protected final String labelName;
    protected final String weightName;
    protected final List<String> features;

    protected AbstractDecisionTree(IndexSpace indexSpace, VariableSpace variableSpace, List<String> features,
                                   List<FeatureExtractor> featureExtractors, String labelName,
                                   String weightName) {
        this.featureExtractors = featureExtractors;
        this.indexSpace = indexSpace;
        this.variableSpace = variableSpace;
        this.labelName = labelName;
        this.weightName = weightName;
        this.features = features;
        featurizer = new StandardFeaturizer(indexSpace, featureExtractors, features,
                null, labelName, weightName);
    }

    public StandardLearningInstance getLearningInstance(LearningInstance ins) {
        return (StandardLearningInstance) ins;
    }

    public LearningInstance featurize(JsonNode entity, boolean update) {
        return featurizer.featurize(entity, update);
    }

    public void publishModel() {
        indexSpace.publishSpaceVersion();
        variableSpace.publishSpaceVersion();
    }
}
