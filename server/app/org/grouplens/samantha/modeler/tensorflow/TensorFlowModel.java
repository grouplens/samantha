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
import org.grouplens.samantha.modeler.model.AbstractLearningModel;
import org.grouplens.samantha.modeler.solver.IdentityFunction;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.modeler.solver.StochasticOracle;
import org.grouplens.samantha.modeler.model.IndexSpace;
import org.grouplens.samantha.modeler.model.UncollectableModel;
import org.grouplens.samantha.modeler.model.VariableSpace;
import org.grouplens.samantha.server.exception.BadRequestException;
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
    transient protected Graph graph;
    transient protected Session session;
    protected final List<FeatureExtractor> featureExtractors;
    protected final String lossOperationName;
    protected final String updateOperationName;
    protected final String outputOperationName;
    protected final String initOperationName;
    protected final List<String> groupKeys;
    protected final List<String> indexKeys;

    public TensorFlowModel(Graph graph,
                           IndexSpace indexSpace, VariableSpace variableSpace,
                           List<FeatureExtractor> featureExtractors,
                           String lossOperationName, String updateOperationName,
                           String outputOperationName, String initOperationName,
                           List<String> groupKeys, List<String> indexKeys) {
        super(indexSpace, variableSpace);
        this.graph = graph;
        this.session = new Session(graph);
        this.groupKeys = groupKeys;
        this.featureExtractors = featureExtractors;
        this.lossOperationName = lossOperationName;
        this.updateOperationName = updateOperationName;
        this.outputOperationName = outputOperationName;
        this.initOperationName = initOperationName;
        this.indexKeys = indexKeys;
        session.runner().addTarget(initOperationName).run();
    }

    public double[] predict(LearningInstance ins) {
        List<LearningInstance> instances = new ArrayList<>(1);
        instances.add(ins);
        double[][] preds = predict(instances);
        double[] insPreds = new double[preds[0].length];
        for (int i=0; i<preds[0].length; i++) {
            insPreds[i] = preds[0][i];
        }
        return insPreds;
    }

    public double[][] predict(List<LearningInstance> instances) {
        Session.Runner runner = session.runner();
        Map<String, Tensor> feedDict = getFeedDict(instances);
        for (Map.Entry<String, Tensor> entry : feedDict.entrySet()) {
            runner.feed(entry.getKey(), entry.getValue());
        }
        runner.fetch(outputOperationName);
        List<Tensor<?>> results = runner.run();
        for (Map.Entry<String, Tensor> entry : feedDict.entrySet()) {
            entry.getValue().close();
        }
        Tensor<?> tensorOutput = results.get(0);
        if (tensorOutput.numDimensions() != 2) {
            throw new BadRequestException("The TensorFlow model should always predict with a two dimensional tensor.");
        }
        long[] outputShape = tensorOutput.shape();
        double[][] preds = new double[(int)outputShape[0]][(int)outputShape[1]];
        DoubleBuffer buffer = DoubleBuffer.allocate(tensorOutput.numElements());
        tensorOutput.writeTo(buffer);
        int k = 0;
        for (int i=0; i<instances.size(); i++) {
            for (int j=0; j<preds[0].length; j++) {
                preds[i][j] = buffer.get(k++);
            }
        }
        for (int i=0; i<results.size(); i++) {
            results.get(i).close();
        }
        buffer.clear();
        return preds;
    }

    public LearningInstance featurize(JsonNode entity, boolean update) {
        Map<String, List<Feature>> feaMap = FeaturizerUtilities.getFeatureMap(entity, true,
                featureExtractors, indexSpace);
        String group = null;
        if (groupKeys != null && groupKeys.size() > 0) {
            group = FeatureExtractorUtilities.composeConcatenatedKey(entity, groupKeys);
        }
        TensorFlowInstance instance = new TensorFlowInstance(group);
        for (Map.Entry<String, List<Feature>> entry : feaMap.entrySet()) {
            DoubleList doubles = new DoubleArrayList();
            IntList ints = new IntArrayList();
            for (Feature feature : entry.getValue()) {
                doubles.add(feature.getValue());
                ints.add(feature.getIndex());
            }
            double[] darr = doubles.toDoubleArray();
            instance.putValues(entry.getKey(), darr);
            int[] iarr = ints.toIntArray();
            instance.putIndices(entry.getKey(), iarr);
        }
        return instance;
    }

    public int getFeatureBuffer(List<LearningInstance> instances, Map<String, Integer> numCols,
                                 Map<String, DoubleBuffer> doubleBufferMap,
                                 Map<String, IntBuffer> intBufferMap) {
        int batch = instances.size();
        for (LearningInstance instance : instances) {
            TensorFlowInstance tfins = (TensorFlowInstance) instance;
            for (Map.Entry<String, int[]> entry : tfins.getName2Indices().entrySet()) {
                String name = entry.getKey();
                int[] values = entry.getValue();
                int cur = numCols.getOrDefault(name, 0);
                if (values.length > cur) {
                    numCols.put(name, values.length);
                }
            }
        }
        for (LearningInstance instance : instances) {
            TensorFlowInstance tfins = (TensorFlowInstance) instance;
            for (Map.Entry<String, double[]> entry : tfins.getName2Values().entrySet()) {
                String name = entry.getKey();
                double[] values = entry.getValue();
                DoubleBuffer buffer = DoubleBuffer.allocate(batch * numCols.get(name));
                buffer.put(values);
                doubleBufferMap.put(name, buffer);
            }
            for (Map.Entry<String, int[]> entry : tfins.getName2Indices().entrySet()) {
                String name = entry.getKey();
                int[] values = entry.getValue();
                IntBuffer buffer = IntBuffer.allocate(batch * numCols.get(name));
                buffer.put(values);
                intBufferMap.put(name, buffer);
            }
        }
        return batch;
    }

    private Map<String, Tensor> getFeedDict(List<LearningInstance> instances) {
        Map<String, Integer> numCols = new HashMap<>();
        Map<String, DoubleBuffer> doubleBufferMap = new HashMap<>();
        Map<String, IntBuffer> intBufferMap = new HashMap<>();
        int batch = getFeatureBuffer(instances, numCols, doubleBufferMap, intBufferMap);
        Map<String, Tensor> tensorMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : numCols.entrySet()) {
            String name = entry.getKey();
            int numCol = entry.getValue();
            long[] shape = {batch, numCol};
            if (doubleBufferMap.containsKey(name)) {
                DoubleBuffer buffer = doubleBufferMap.get(name);
                buffer.rewind();
                tensorMap.put(name + "_val", Tensor.create(shape, buffer));
            }
            if (intBufferMap.containsKey(name)) {
                IntBuffer buffer = intBufferMap.get(name);
                buffer.rewind();
                tensorMap.put(name + "_idx", Tensor.create(shape, buffer));
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
        List<Tensor<?>> results = runner.run();
        for (Map.Entry<String, Tensor> entry : feedDict.entrySet()) {
            entry.getValue().close();
        }
        StochasticOracle oracle = new StochasticOracle(results.get(0).doubleValue(), 0.0, 1.0);
        results.get(0).close();
        List<StochasticOracle> oracles = new ArrayList<>(1);
        oracles.add(oracle);
        return oracles;
    }

    public ObjectiveFunction getObjectiveFunction() {
        return new IdentityFunction();
    }

    public void destroyModel() {
        if (session != null) {
            session.close();
        }
        if (graph != null) {
            graph.close();
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        byte[] graphDef = graph.toGraphDef();
        stream.writeInt(graphDef.length);
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
