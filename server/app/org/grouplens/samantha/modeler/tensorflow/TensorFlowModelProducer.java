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

package org.grouplens.samantha.modeler.tensorflow;

import org.apache.commons.io.IOUtils;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.model.IndexSpace;
import org.grouplens.samantha.modeler.model.SpaceMode;
import org.grouplens.samantha.modeler.model.SpaceProducer;
import org.grouplens.samantha.modeler.model.VariableSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tensorflow.Graph;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class TensorFlowModelProducer {
    private static Logger logger = LoggerFactory.getLogger(TensorFlowModelProducer.class);

    @Inject
    private SpaceProducer spaceProducer;

    @Inject
    private TensorFlowModelProducer() {}

    public TensorFlowModelProducer(SpaceProducer spaceProducer) {
        this.spaceProducer = spaceProducer;
    }

    private IndexSpace getIndexSpace(String spaceName, SpaceMode spaceMode, List<String> indexKeys) {
        IndexSpace indexSpace = spaceProducer.getIndexSpace(spaceName, spaceMode);
        for (String indexKey : indexKeys) {
            indexSpace.requestKeyMap(indexKey);
            indexSpace.setKey(indexKey, TensorFlowModel.OOV);
        }
        return indexSpace;
    }

    private VariableSpace getVariableSpace(String spaceName, SpaceMode spaceMode) {
        VariableSpace variableSpace = spaceProducer.getVariableSpace(spaceName, spaceMode);
        return variableSpace;
    }

    static public Graph loadTensorFlowGraph(String graphDefFilePath) {
        Graph graph = null;
        try {
            byte[] graphDef;
            graphDef = IOUtils.toByteArray(new FileInputStream(graphDefFilePath));
            graph = new Graph();
            graph.importGraphDef(graphDef);
        } catch (IOException e) {
            logger.warn("Loading TensorFlow graph definition file error: {}.", e.getMessage());
        }
        return graph;
    }

    static public SavedModelBundle loadTensorFlowSavedModel(String exportDir) {
        SavedModelBundle savedModel = null;
        if (new File(exportDir).exists()) {
            savedModel = SavedModelBundle.load(exportDir, TensorFlowModel.SAVED_MODEL_TAG);
        } else {
            logger.warn("TensorFlow exported model dir does not exist: {}.", exportDir);
        }
        return savedModel;
    }

    public TensorFlowModel createTensorFlowModelModelFromGraphDef(
            String modelName,
            SpaceMode spaceMode,
            String graphDefFilePath,
            List<String> groupKeys,
            List<List<String>> equalSizeChecks,
            List<String> indexKeys,
            List<FeatureExtractor> featureExtractors,
            String predItemFea,
            String lossOper,
            String updateOper,
            String outputOper,
            String initOper,
            String topKOper,
            String topKId,
            String topKValue,
            String itemIndex) {
        IndexSpace indexSpace = getIndexSpace(modelName, spaceMode, indexKeys);
        VariableSpace variableSpace = getVariableSpace(modelName, spaceMode);
        Graph graph = loadTensorFlowGraph(graphDefFilePath);
        Session session = null;
        if (graph != null) {
            session = new Session(graph);
            session.runner().addTarget(initOper).run();
        }
        return new TensorFlowModel(graph, session, graphDefFilePath, null, indexSpace, variableSpace,
                featureExtractors, predItemFea, lossOper, updateOper, topKId, itemIndex,
                topKValue, outputOper, topKOper, initOper, groupKeys, equalSizeChecks);
    }

    public TensorFlowModel createTensorFlowModelModelFromExportDir(
            String modelName,
            SpaceMode spaceMode,
            String exportDir,
            List<String> groupKeys,
            List<List<String>> equalSizeChecks,
            List<String> indexKeys,
            List<FeatureExtractor> featureExtractors,
            String predItemFea,
            String lossOper,
            String updateOper,
            String outputOper,
            String initOper,
            String topKOper,
            String topKId,
            String topKValue,
            String itemIndex) {
        IndexSpace indexSpace = getIndexSpace(modelName, spaceMode, indexKeys);
        VariableSpace variableSpace = getVariableSpace(modelName, spaceMode);
        SavedModelBundle savedModel = loadTensorFlowSavedModel(exportDir);
        Session session = null;
        Graph graph = null;
        if (savedModel != null) {
            session = savedModel.session();
            graph = savedModel.graph();
        }
        return new TensorFlowModel(graph, session, null, exportDir, indexSpace, variableSpace,
                featureExtractors, predItemFea, lossOper, updateOper, topKId, itemIndex,
                topKValue, outputOper, topKOper, initOper, groupKeys, equalSizeChecks);
    }
}
