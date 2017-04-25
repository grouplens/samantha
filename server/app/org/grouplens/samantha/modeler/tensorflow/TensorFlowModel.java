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

package org.grouplens.samantha.modeler.tensorflow;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.featurizer.Featurizer;
import org.grouplens.samantha.modeler.solver.AbstractLearningModel;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.modeler.solver.StochasticOracle;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.tensorflow.Graph;

import java.util.List;

public class TensorFlowModel extends AbstractLearningModel implements Featurizer {
    protected final Graph graph;
    protected final IndexSpace indexSpace;
    protected final VariableSpace variableSpace;

    public TensorFlowModel(Graph graph,
                           IndexSpace indexSpace, VariableSpace variableSpace,
                           List<FeatureExtractor> featureExtractors,
                           List<String> features) {
        super(indexSpace, variableSpace);
        this.graph = graph;
        this.indexSpace = indexSpace;
        this.variableSpace = variableSpace;
    }

    public double predict(LearningInstance ins) {
        return 0.0;
    }

    public LearningInstance featurize(JsonNode entity, boolean update) {
        return null;
    }

    public void publishModel() {}

    public void destroy() {
        graph.close();
    }

    public List<StochasticOracle> getStochasticOracle(List<LearningInstance> instances) {
        throw new BadRequestException("getStochasticOracle is not supported.");
    }

    public ObjectiveFunction getObjectiveFunction() {
        throw new BadRequestException("getObjectiveFunction is not supported.");
    }
}
