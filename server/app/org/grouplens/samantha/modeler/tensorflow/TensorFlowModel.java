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
import org.grouplens.samantha.modeler.featurizer.Feature;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.featurizer.Featurizer;
import org.grouplens.samantha.modeler.featurizer.FeaturizerUtilities;
import org.grouplens.samantha.modeler.solver.AbstractLearningModel;
import org.grouplens.samantha.modeler.solver.IdentityFunction;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.modeler.solver.StochasticOracle;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.UncollectableModel;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.tensorflow.Graph;
import org.tensorflow.Session;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

public class TensorFlowModel extends AbstractLearningModel implements Featurizer, UncollectableModel {
    transient protected Graph graph;
    transient protected Session session;
    protected final List<FeatureExtractor> featureExtractors;

    public TensorFlowModel(Graph graph,
                           IndexSpace indexSpace, VariableSpace variableSpace,
                           List<FeatureExtractor> featureExtractors,
                           List<String> features) {
        super(indexSpace, variableSpace);
        this.graph = graph;
        this.session = new Session(graph);
        this.featureExtractors = featureExtractors;
    }

    public double predict(LearningInstance ins) {
        return 0.0;
    }

    public LearningInstance featurize(JsonNode entity, boolean update) {
        Map<String, List<Feature>> feaMap = FeaturizerUtilities.getFeatureMap(entity, update,
                featureExtractors, indexSpace);
        TensorFlowInstance instance = new TensorFlowInstance();
        return instance;
    }

    public void publishModel() {}

    public List<StochasticOracle> getStochasticOracle(List<LearningInstance> instances) {
        return null;
    }

    public ObjectiveFunction getObjectiveFunction() {
        return new IdentityFunction();
    }

    public void destroyModel() {
        if (graph != null) {
            graph.close();
        }
        if (session != null) {
            session.close();
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        byte[] graphDef = graph.toGraphDef();
        stream.write(graphDef.length);
        stream.write(graph.toGraphDef());
    }

    private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
        stream.defaultReadObject();
        int len = stream.readInt();
        byte[] graphDef = new byte[len];
        stream.readFully(graphDef);
        graph = new Graph();
        graph.importGraphDef(graphDef);
        session = new Session(graph);
    }
}
