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

import org.apache.commons.io.IOUtils;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.model.IndexSpace;
import org.grouplens.samantha.modeler.model.SpaceMode;
import org.grouplens.samantha.modeler.model.SpaceProducer;
import org.grouplens.samantha.modeler.model.VariableSpace;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.tensorflow.Graph;

import javax.inject.Inject;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class TensorFlowModelProducer {
    @Inject
    private SpaceProducer spaceProducer;

    @Inject
    private TensorFlowModelProducer() {}

    public TensorFlowModelProducer(SpaceProducer spaceProducer) {
        this.spaceProducer = spaceProducer;
    }

    private IndexSpace getIndexSpace(String spaceName, SpaceMode spaceMode) {
        IndexSpace indexSpace = spaceProducer.getIndexSpace(spaceName, spaceMode);
        return indexSpace;
    }

    private VariableSpace getVariableSpace(String spaceName, SpaceMode spaceMode) {
        VariableSpace variableSpace = spaceProducer.getVariableSpace(spaceName, spaceMode);
        return variableSpace;
    }

    public TensorFlowModel createTensorFlowModelModelFromGraphDef(String modelName,
                                                                  SpaceMode spaceMode,
                                                                  String graphDefFilePath,
                                                                  List<String> groupKeys,
                                                                  List<String> indexKeys,
                                                                  List<FeatureExtractor> featureExtractors,
                                                                  String lossOperationName,
                                                                  String updateOperationName,
                                                                  String outputOperationName,
                                                                  String initOperationName) {
        IndexSpace indexSpace = getIndexSpace(modelName, spaceMode);
        for (String indexKey : indexKeys) {
            indexSpace.requestKeyMap(indexKey);
            indexSpace.setKey(indexKey, TensorFlowModel.OOV);
        }
        VariableSpace variableSpace = getVariableSpace(modelName, spaceMode);
        byte[] graphDef;
        try {
            graphDef = IOUtils.toByteArray(new FileInputStream(graphDefFilePath));
        } catch (IOException e) {
            throw new BadRequestException(e);
        }
        Graph graph = new Graph();
        graph.importGraphDef(graphDef);
        return new TensorFlowModel(graph, indexSpace, variableSpace,
                featureExtractors, lossOperationName,
                updateOperationName, outputOperationName, initOperationName,
                groupKeys, indexKeys);
    }
}
