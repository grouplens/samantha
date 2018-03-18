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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.grouplens.samantha.FakeApplication;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.featurizer.SeparatedStringExtractor;
import org.grouplens.samantha.modeler.featurizer.SeparatedStringSizeExtractor;
import org.grouplens.samantha.modeler.model.SpaceMode;
import org.grouplens.samantha.modeler.model.SpaceProducer;
import org.grouplens.samantha.modeler.tensorflow.TensorFlowModel;
import org.grouplens.samantha.modeler.tensorflow.TensorFlowModelProducer;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.indexer.MockIndexer;
import org.grouplens.samantha.server.io.RequestContext;
import org.junit.Test;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;
import play.test.WithApplication;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;


public class TensorFlowBatchIndexerTest extends WithApplication {
    private final Configuration config = FakeApplication.instance().configuration();
    private final Injector injector = FakeApplication.instance().injector();

    @Test
    public void testTensorFlowBatchIndex() {
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        MockIndexer mockIndexer = new MockIndexer(
                config, configService, injector, "daoConfig", config);
        SpaceProducer spaceProducer = injector.instanceOf(SpaceProducer.class);
        List<FeatureExtractor> featureExtractors = new ArrayList<>();
        FeatureExtractor itemExtractor = new SeparatedStringExtractor(
                "ITEM", "item", "item", "\\|",
                false, null, null
        );
        featureExtractors.add(itemExtractor);
        FeatureExtractor attrExtractor = new SeparatedStringExtractor(
                "ATTR", "attr", "attr", "\\|",
                false, "null", null
        );
        featureExtractors.add(attrExtractor);
        FeatureExtractor sizeExtractor = new SeparatedStringSizeExtractor(
                "SEQ_LEN", "item", "sequence_length",
                "|", null
        );
        featureExtractors.add(sizeExtractor);
        TensorFlowModel model = new TensorFlowModelProducer(spaceProducer)
                .createTensorFlowModelModelFromGraphDef(
                        "name", SpaceMode.DEFAULT, "shouldNotExist.graph",
                        null, new ArrayList<>(), null,
                        Lists.newArrayList("ITEM", "ATTR", "SEQ_LEN"),
                        featureExtractors, "loss", "update",
                        "output", "init", "top_k",
                        "topKId", "topKValue", "ITEM");
        TensorFlowBatchIndexer batchIndexer = new TensorFlowBatchIndexer(
                configService, config, injector, config, "daoConfig", mockIndexer,
                model, 1, "tstamp");
        ArrayNode batch = Json.newArray();
        ObjectNode user1 = Json.newObject();
        user1.put("item", "20|49|10|2|4");
        user1.put("attr", "jid|cjk|je|je|cjk");
        batch.add(user1);
        ObjectNode user2 = Json.newObject();
        user2.put("item", "14|19|2|5|20|15|2");
        user2.put("attr", "cjk|mn|je|lk|jid|null|je");
        batch.add(user2);
        RequestContext requestContext = new RequestContext(Json.newObject(), "test");
        batchIndexer.index(batch, requestContext);
        ArrayNode indexed = mockIndexer.getIndexed();
        assertEquals("1,2,3,4,5,6,7,4,8,1,9,4", indexed.get(0).get("item_idx").asText());
        assertEquals("1,2,3,3,2,2,4,3,5,1,6,3", indexed.get(0).get("attr_idx").asText());
        assertEquals("5.0,7.0", indexed.get(0).get("sequence_length_val").asText());
        batch.removeAll();
        indexed.removeAll();
        ObjectNode item1 = Json.newObject();
        item1.put("item", "20");
        item1.put("attr", "jid");
        batch.add(item1);
        ObjectNode item2 = Json.newObject();
        item2.put("item", "15");
        batch.add(item2);
        ObjectNode item3 = Json.newObject();
        item3.put("item", "40");
        item3.put("attr", "cjk");
        batch.add(item3);
        ObjectNode item4 = Json.newObject();
        item4.put("item", "41");
        item4.put("attr", "djkfds");
        batch.add(item4);
        batchIndexer.index(batch, requestContext);
        assertEquals("1,9,10,11", indexed.get(0).get("item_idx").asText());
        assertEquals("1,6,2,7", indexed.get(0).get("attr_idx").asText());
    }
}
