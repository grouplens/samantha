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

package org.grouplens.samantha.server.featurizer;

import org.grouplens.samantha.modeler.featurizer.DisplayActionGroupExtractor;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class DisplayActionGroupExtractorConfig implements FeatureExtractorConfig {
    private final String index;
    private final String sizeFeaIndex;
    private final String attr;
    private final String inGrpRank;
    private final String fea;
    private final String sizeFea;
    private final List<String> actionIndices;
    private final List<String> actionAttrs;
    private final List<String> actionFeas;
    private final String displayActionIndex;
    private final String displayActionFea;
    private final String separator;
    private final boolean normalize;
    private final Integer maxGrpNum;
    private final int grpSize;

    private DisplayActionGroupExtractorConfig(String index,
                                              String sizeFeaIndex,
                                              String attr,
                                              String inGrpRank,
                                              String fea,
                                              String sizeFea,
                                              List<String> actionIndices,
                                              List<String> actionAttrs,
                                              List<String> actionFeas,
                                              String displayActionIndex,
                                              String displayActionFea,
                                              String separator,
                                              boolean normalize,
                                              Integer maxGrpNum,
                                              int grpSize) {
        this.index = index;
        this.attr = attr;
        this.fea = fea;
        this.actionAttrs = actionAttrs;
        this.actionFeas = actionFeas;
        this.actionIndices = actionIndices;
        this.displayActionFea = displayActionFea;
        this.displayActionIndex = displayActionIndex;
        this.separator = separator;
        this.normalize = normalize;
        this.sizeFeaIndex = sizeFeaIndex;
        this.inGrpRank = inGrpRank;
        this.sizeFea = sizeFea;
        this.maxGrpNum = maxGrpNum;
        this.grpSize = grpSize;
    }

    public FeatureExtractor getFeatureExtractor(RequestContext requestContext) {
        return new DisplayActionGroupExtractor(
                index, sizeFeaIndex, attr, fea, sizeFea, actionIndices, actionIndices, actionFeas,
                displayActionIndex, displayActionFea, separator, normalize, maxGrpNum, grpSize, inGrpRank);
    }

    public static FeatureExtractorConfig
            getFeatureExtractorConfig(Configuration extractorConfig,
                                      Injector injector) {
        Boolean normalize = extractorConfig.getBoolean("normalize");
        if (normalize == null) {
            normalize = true;
        }
        return new DisplayActionGroupExtractorConfig(
                extractorConfig.getString("index"),
                extractorConfig.getString("sizeFeaIndex"),
                extractorConfig.getString("attr"),
                extractorConfig.getString("inGrpRank"),
                extractorConfig.getString("fea"),
                extractorConfig.getString("sizeFea"),
                extractorConfig.getStringList("actionIndices"),
                extractorConfig.getStringList("actionAttrs"),
                extractorConfig.getStringList("actionFeas"),
                extractorConfig.getString("displayActionIndex"),
                extractorConfig.getString("displayActionFea"),
                extractorConfig.getString("separator"),
                normalize, extractorConfig.getInt("maxGrpNum"),
                extractorConfig.getInt("grpSize"));
    }
}
