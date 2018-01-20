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

package org.grouplens.samantha.server.tensorflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.tensorflow.TensorFlowModel;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.indexer.AbstractIndexer;
import org.grouplens.samantha.server.indexer.Indexer;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TensorFlowBatchIndexer extends AbstractIndexer {
    private final Indexer indexer;
    private final TensorFlowModel model;
    private final boolean update;
    private final String timestampField;
    //this overrides the parent member
    protected final int batchSize;

    public TensorFlowBatchIndexer(SamanthaConfigService configService,
                                  Configuration config, Injector injector,
                                  Configuration daoConfigs, String daoConfigKey,
                                  Indexer indexer, TensorFlowModel model,
                                  int batchSize, boolean update, String timestampField) {
        super(config, configService, daoConfigs, daoConfigKey, injector);
        this.indexer = indexer;
        this.model = model;
        this.batchSize = batchSize;
        this.update = update;
        this.timestampField = timestampField;
    }

    public ObjectNode getIndexedDataDAOConfig(RequestContext requestContext) {
        return indexer.getIndexedDataDAOConfig(requestContext);
    }

    public void index(JsonNode documents, RequestContext requestContext) {
        int timestamp = (int) (System.currentTimeMillis() / 1000);
        List<LearningInstance> instances = new ArrayList<>();
        JsonNode last;
        if (documents.isArray() && documents.size() > 0) {
            for (JsonNode document : documents) {
                LearningInstance instance = model.featurize(document, update);
                instances.add(instance);
            }
            last = documents.get(documents.size() - 1);
        } else {
            instances.add(model.featurize(documents, update));
            last = documents;
        }
        if (last.has(timestampField)) {
            timestamp = last.get(timestampField).asInt();
        }
        ObjectNode jsonTensors = Json.newObject();
        Map<String, String> tensorMap = model.getStringifiedSparseTensor(instances);
        for (Map.Entry<String, String> entry : tensorMap.entrySet()) {
            jsonTensors.put(entry.getKey(), entry.getValue());
        }
        jsonTensors.put(timestampField, timestamp);
        indexer.index(jsonTensors, requestContext);
    }
}
