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

package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.apache.commons.lang3.StringUtils;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.modeler.model.AbstractLearningModel;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class NegativeSamplingExpander implements EntityExpander {
    final private String itemAttr;
    final private String itemIndex;
    final private String keyPrefix;
    final private String labelAttr;
    final private String separator;
    final private String joiner;
    final private List<String> fillInAttrs;
    final private Integer maxIdx;
    final private Integer maxNumSample;
    final private AbstractLearningModel model;

    public NegativeSamplingExpander(String itemAttr,
                                    String itemIndex,
                                    String keyPrefix,
                                    String labelAttr,
                                    List<String> fillInAttrs,
                                    String separator,
                                    String joiner,
                                    Integer maxIdx,
                                    Integer maxNumSample,
                                    AbstractLearningModel model) {
        this.separator = separator;
        this.labelAttr = labelAttr;
        this.joiner = joiner;
        this.itemAttr = itemAttr;
        this.fillInAttrs = fillInAttrs;
        this.itemIndex = itemIndex;
        this.keyPrefix = keyPrefix;
        this.model = model;
        this.maxIdx = maxIdx;
        this.maxNumSample = maxNumSample;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        ModelService modelService = injector.instanceOf(ModelService.class);
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        configService.getPredictor(expanderConfig.getString("predictorName"), requestContext);
        AbstractLearningModel model = (AbstractLearningModel) modelService.getModel(
                requestContext.getEngineName(), expanderConfig.getString("modelName"));
        String keyPrefix = expanderConfig.getString("keyPrefix");
        if (keyPrefix == null) {
            keyPrefix = expanderConfig.getString("itemAttr");
        }
        return new NegativeSamplingExpander(
                expanderConfig.getString("itemAttr"),
                expanderConfig.getString("itemIndex"), keyPrefix,
                expanderConfig.getString("labelAttr"),
                expanderConfig.getStringList("fillInAttrs"),
                expanderConfig.getString("separator"),
                expanderConfig.getString("joiner"),
                expanderConfig.getInt("maxIdx"),
                expanderConfig.getInt("maxNumSample"), model);
    }

    private IntList getSampledIndices(IntSet trues, int maxVal) {
        IntList samples = new IntArrayList();
        int num = trues.size();
        if (maxNumSample != null) {
            num = maxNumSample;
        }
        for (int i=0; i<num; i++) {
            int dice = new Random().nextInt(maxVal);
            if (!trues.contains(dice)) {
                samples.add(dice);
            }
        }
        return samples;
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        int indexSize = model.getKeyMapSize(itemIndex);
        int maxVal = indexSize;
        if (maxIdx != null && maxIdx < indexSize) {
            maxVal = maxIdx;
        }
        for (ObjectNode entity : initialResult) {
            String itemStr = entity.get(itemAttr).asText();
            if (!"".equals(itemStr)) {
                String[] items = itemStr.split(separator, -1);
                IntSet trues = new IntOpenHashSet();
                for (String item : items) {
                    String key = FeatureExtractorUtilities.composeKey(keyPrefix, item);
                    if (model.containsKey(itemIndex, key)) {
                        trues.add(model.getIndexForKey(itemIndex, key));
                    } else {
                        trues.add(0);
                    }
                }
                IntList samples = getSampledIndices(trues, maxVal);
                List<String> itemArr = Lists.newArrayList(itemStr);
                List<String> labelArr = Lists.newArrayList(entity.get(labelAttr).asText());
                for (int sample : samples) {
                    Map<String, String> key2val = FeatureExtractorUtilities.decomposeKey(
                            (String)model.getKeyForIndex(itemIndex, sample));
                    itemArr.add(key2val.get(keyPrefix));
                    labelArr.add("0");
                }
                if (fillInAttrs != null && fillInAttrs.size() > 0 && samples.size() > 0) {
                    for (int i=0; i<fillInAttrs.size(); i++) {
                        String fillStr = entity.get(fillInAttrs.get(i)).asText();
                        String[] fills = fillStr.split(separator, -1);
                        List<String> fillEls = Lists.newArrayList(fillStr);
                        for (int j=0; j<samples.size(); j++) {
                            fillEls.add(fills[fills.length - 1]);
                        }
                        entity.put(fillInAttrs.get(i), StringUtils.join(fillEls, joiner));
                    }
                }
                entity.put(labelAttr, StringUtils.join(labelArr, joiner));
                entity.put(itemAttr, StringUtils.join(itemArr, joiner));
            }
        }
        return initialResult;
    }
}
