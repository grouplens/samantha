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
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.featurizer.*;
import org.grouplens.samantha.modeler.solver.AbstractLearningModel;
import org.grouplens.samantha.modeler.solver.IdentityFunction;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.modeler.solver.StochasticOracle;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.UncollectableModel;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TensorFlowModel extends AbstractLearningModel implements Featurizer, UncollectableModel {
    private static final long serialVersionUID = 1L;
    public static final String indexKey = "TENSOR_FLOW";
    transient protected Graph graph;
    transient protected Session session;
    protected final List<FeatureExtractor> featureExtractors;
    protected final String lossOperationName;
    protected final String updateOperationName;
    protected final String outputOperationName;
    protected final String initOperationName;
    protected final List<String> groupKeys;
    protected final Map<String, List<String>> name2intfeas;
    protected final Map<String, List<String>> name2doublefeas;

    public TensorFlowModel(Graph graph,
                           IndexSpace indexSpace, VariableSpace variableSpace,
                           List<FeatureExtractor> featureExtractors,
                           String lossOperationName, String updateOperationName,
                           String outputOperationName, String initOperationName,
                           List<String> groupKeys,
                           Map<String, List<String>> name2doublefeas,
                           Map<String, List<String>> name2intfeas) {
        super(indexSpace, variableSpace);
        this.graph = graph;
        this.session = new Session(graph);
        this.groupKeys = groupKeys;
        this.name2doublefeas = name2doublefeas;
        this.name2intfeas = name2intfeas;
        this.featureExtractors = featureExtractors;
        this.lossOperationName = lossOperationName;
        this.updateOperationName = updateOperationName;
        this.outputOperationName = outputOperationName;
        this.initOperationName = initOperationName;
        session.runner().addTarget(initOperationName).run();
    }

    //TODO: this is assuming regression, i.e. the output is always a single scalar value; change to/or add another double[], and also accepts List<LearningInstance>
    public double predict(LearningInstance ins) {
        List<LearningInstance> instances = new ArrayList<>(1);
        instances.add(ins);
        Session.Runner runner = session.runner();
        Map<String, Tensor> feedDict = getFeedDict(instances);
        for (Map.Entry<String, Tensor> entry : feedDict.entrySet()) {
            runner.feed(entry.getKey(), entry.getValue());
        }
        runner.fetch(outputOperationName);
        List<Tensor> results = runner.run();
        return results.get(0).doubleValue();
    }

    public LearningInstance featurize(JsonNode entity, boolean update) {
        Map<String, List<Feature>> feaMap = FeaturizerUtilities.getFeatureMap(entity, update,
                featureExtractors, indexSpace);
        String group = null;
        if (groupKeys != null && groupKeys.size() > 0) {
            group = FeatureExtractorUtilities.composeConcatenatedKey(entity, groupKeys);
        }
        TensorFlowInstance instance = new TensorFlowInstance(group);
        for (Map.Entry<String, List<String>> entry : name2doublefeas.entrySet()) {
            DoubleList doubles = new DoubleArrayList();
            for (String feaName : entry.getValue()) {
                for (Feature feature : feaMap.get(feaName)) {
                    doubles.add(feature.getValue());
                }
            }
            double[] arr = doubles.toDoubleArray();
            instance.putDoubles(entry.getKey(), arr);
        }
        for (Map.Entry<String, List<String>> entry : name2intfeas.entrySet()) {
            IntList ints = new IntArrayList();
            for (String feaName : entry.getValue()) {
                for (Feature feature : feaMap.get(feaName)) {
                    ints.add(feature.getIndex());
                }
            }
            int[] arr = ints.toIntArray();
            instance.putInts(entry.getKey(), arr);
        }
        return instance;
    }

    private Map<String, Tensor> getFeedDict(List<LearningInstance> instances) {
        int batch = instances.size();
        Map<String, Integer> numCols = new HashMap<>();
        Map<String, DoubleBuffer> doubleBufferMap = new HashMap<>();
        Map<String, IntBuffer> intBufferMap = new HashMap<>();
        for (LearningInstance instance : instances) {
            TensorFlowInstance tfins = (TensorFlowInstance) instance;
            for (Map.Entry<String, double[]> entry : tfins.getName2Doubles().entrySet()) {
                String name = entry.getKey();
                double[] values = entry.getValue();
                doubleBufferMap.putIfAbsent(name, DoubleBuffer.allocate(batch * values.length));
                DoubleBuffer buffer = doubleBufferMap.get(name);
                buffer.put(values);
                numCols.putIfAbsent(name, values.length);
            }
            for (Map.Entry<String, int[]> entry : tfins.getName2Ints().entrySet()) {
                String name = entry.getKey();
                int[] values = entry.getValue();
                intBufferMap.putIfAbsent(name, IntBuffer.allocate(batch * values.length));
                IntBuffer buffer = intBufferMap.get(name);
                buffer.put(values);
                numCols.putIfAbsent(name, values.length);
            }
        }
        Map<String, Tensor> tensorMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : numCols.entrySet()) {
            String name = entry.getKey();
            int numCol = entry.getValue();
            long[] shape = {batch, numCol};
            if (doubleBufferMap.containsKey(name)) {
                DoubleBuffer buffer = doubleBufferMap.get(name);
                buffer.rewind();
                tensorMap.put(name, Tensor.create(shape, buffer));
            } else if (intBufferMap.containsKey(name)) {
                IntBuffer buffer = intBufferMap.get(name);
                buffer.rewind();
                tensorMap.put(name, Tensor.create(shape, buffer));
            }
        }
        return tensorMap;
    }

    public List<StochasticOracle> getStochasticOracle(List<LearningInstance> instances) {
        Session.Runner runner = session.runner();
        Map<String, Tensor> feedDict = getFeedDict(instances);
        for (Map.Entry<String, Tensor> entry : feedDict.entrySet()) {
            runner.feed(entry.getKey(), entry.getValue());
        }
        runner.addTarget(updateOperationName).fetch(lossOperationName);
        List<Tensor> results = runner.run();
        StochasticOracle oracle = new StochasticOracle(results.get(0).doubleValue(), 0.0, 1.0);
        List<StochasticOracle> oracles = new ArrayList<>(1);
        oracles.add(oracle);
        return oracles;
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
        session.runner().addTarget(initOperationName).run();
    }
}
