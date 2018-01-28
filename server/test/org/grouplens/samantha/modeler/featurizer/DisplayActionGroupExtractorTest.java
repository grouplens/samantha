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

package org.grouplens.samantha.modeler.featurizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.grouplens.samantha.modeler.model.IndexSpace;
import org.grouplens.samantha.modeler.model.SynchronizedIndexSpace;
import org.grouplens.samantha.modeler.tensorflow.TensorFlowModel;
import org.junit.*;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class DisplayActionGroupExtractorTest {

    private JsonNode setUp() {
        ObjectNode entity = Json.newObject();
        entity.put("itemId", "111,112,113,114,115,116,117,118,119,120");
        entity.put("rank", "0,1,2,3,4,5,0,1,2,3");
        entity.put("click", "0,0,0,1,0,1,0,0,0,0");
        entity.put("rate", "1,0,0,0,0,0,0,1,0,0");
        return entity;
    }

    @Test
    public void testExtractGroupFeatures() {
        JsonNode entity = setUp();
        DisplayActionGroupExtractor extractor = new DisplayActionGroupExtractor(
                "ITEM", null, "itemId", "item", null,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                null, null, ",", true,
                null, 4, "rank");
        IndexSpace indexSpace = new SynchronizedIndexSpace();
        indexSpace.requestKeyMap("ITEM");
        indexSpace.setKey("ITEM", TensorFlowModel.OOV);
        Map<String, List<Feature>> feaMap = extractor.extract(entity, true, indexSpace);
        assertEquals(1, feaMap.size());
        assertEquals(true, feaMap.containsKey("item"));
        assertEquals(12, feaMap.get("item").size());
        int[] indices = {1, 2, 3, 4, 5, 6, 0, 0, 7, 8, 9, 10};
        for (int i=0; i<feaMap.get("item").size(); i++) {
            assertEquals(feaMap.get("item").get(i).getIndex(), indices[i]);
            assertEquals(1.0 / Math.sqrt(3.0), feaMap.get("item").get(i).getValue(), 0.0);
        }
    }

    @Test
    public void testExtractSizeFeature() {
        JsonNode entity = setUp();
        DisplayActionGroupExtractor extractor = new DisplayActionGroupExtractor(
                "ITEM", "SEQ_LEN", "itemId", "item", "sequence_length",
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                null, null, ",", false,
                null, 4, "rank");
        IndexSpace indexSpace = new SynchronizedIndexSpace();
        List<String> indices = Lists.newArrayList("ITEM", "SEQ_LEN");
        for (String index : indices) {
            indexSpace.requestKeyMap(index);
            indexSpace.setKey(index, TensorFlowModel.OOV);
        }
        Map<String, List<Feature>> feaMap = extractor.extract(entity, true, indexSpace);
        assertEquals(2, feaMap.size());
        assertEquals(true, feaMap.containsKey("sequence_length"));
        assertEquals(1, feaMap.get("sequence_length").size());
        assertEquals(1, feaMap.get("sequence_length").get(0).getIndex());
        assertEquals(12.0, feaMap.get("sequence_length").get(0).getValue(), 0.0);
    }

    @Test
    public void testExtractActionFeatures() {
        JsonNode entity = setUp();
        List<String> actionIndices = Lists.newArrayList("CLICK", "RATE");
        List<String> actionAttrs = Lists.newArrayList("click", "rate");
        List<String> actionFeas = Lists.newArrayList("click", "rate");
        List<Boolean> extractBools = Lists.newArrayList(false, false);
        DisplayActionGroupExtractor extractor = new DisplayActionGroupExtractor(
                "ITEM", "SEQ_LEN", "itemId", "item", "sequence_length",
                actionIndices, actionAttrs, actionFeas, extractBools,
                null, null, ",", false,
                null, 4, "rank");
        IndexSpace indexSpace = new SynchronizedIndexSpace();
        List<String> indices = Lists.newArrayList("ITEM", "SEQ_LEN", "CLICK", "RATE");
        for (String index : indices) {
            indexSpace.requestKeyMap(index);
            indexSpace.setKey(index, TensorFlowModel.OOV);
        }
        Map<String, List<Feature>> feaMap = extractor.extract(entity, true, indexSpace);
        assertEquals(4, feaMap.size());
        assertEquals(true, feaMap.containsKey("click"));
        assertEquals(true, feaMap.containsKey("rate"));
        assertEquals(1, feaMap.get("sequence_length").size());
        assertEquals(12, feaMap.get("item").size());
        assertEquals(12, feaMap.get("click").size());
        assertEquals(12, feaMap.get("rate").size());
        assertEquals(1, feaMap.get("sequence_length").get(0).getIndex());
        assertEquals(12.0, feaMap.get("sequence_length").get(0).getValue(), 0.0);
        int[] feaIdx = {1, 2, 3, 4, 5, 6, 0, 0, 7, 8, 9, 10};
        int[] clickIdx = {0, 0, 0, 1, 0, 2, 0, 0, 0, 0, 0, 0};
        int[] rateIdx = {1, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0};
        for (int i=0; i<feaMap.get("item").size(); i++) {
            assertEquals(feaMap.get("item").get(i).getIndex(), feaIdx[i]);
            assertEquals(feaMap.get("click").get(i).getIndex(), clickIdx[i]);
            assertEquals(feaMap.get("rate").get(i).getIndex(), rateIdx[i]);
            assertEquals(1.0, feaMap.get("item").get(i).getValue(), 0.0);
            assertEquals(1.0, feaMap.get("click").get(i).getValue(), 0.0);
            assertEquals(1.0, feaMap.get("click").get(i).getValue(), 0.0);
        }
    }

    @Test
    public void testExtractBoolFeatures() {

    }

    @Test
    public void testExtractDisplayActionFeatures() {

    }

    @Test
    public void testExtractMaxGroupFeatures() {
        JsonNode entity = setUp();
        DisplayActionGroupExtractor extractor = new DisplayActionGroupExtractor(
                "ITEM", "SEQ_LEN", "itemId", "item", "sequence_length",
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                null, null, ",", false,
                2, 4, "rank");
        IndexSpace indexSpace = new SynchronizedIndexSpace();
        List<String> indices = Lists.newArrayList("ITEM", "SEQ_LEN");
        for (String index : indices) {
            indexSpace.requestKeyMap(index);
            indexSpace.setKey(index, TensorFlowModel.OOV);
        }
        Map<String, List<Feature>> feaMap = extractor.extract(entity, true, indexSpace);
        assertEquals(2, feaMap.size());
        assertEquals(true, feaMap.containsKey("sequence_length"));
        assertEquals(1, feaMap.get("sequence_length").size());
        assertEquals(1, feaMap.get("sequence_length").get(0).getIndex());
        assertEquals(8.0, feaMap.get("sequence_length").get(0).getValue(), 0.0);
        assertEquals(true, feaMap.containsKey("item"));
        assertEquals(8, feaMap.get("item").size());
        int[] feaIdx = {1, 2, 3, 4, 5, 6, 7, 8};
        for (int i=0; i<feaMap.get("item").size(); i++) {
            assertEquals(feaMap.get("item").get(i).getIndex(), feaIdx[i]);
            assertEquals(1.0, feaMap.get("item").get(i).getValue(), 0.0);
        }
    }
}
