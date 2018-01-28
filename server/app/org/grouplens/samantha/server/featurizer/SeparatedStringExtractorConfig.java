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

import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.featurizer.SeparatedStringExtractor;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class SeparatedStringExtractorConfig implements FeatureExtractorConfig {
    private final String indexName;
    private final String feaName;
    private final String attrName;
    private final String separator;
    private final boolean normalize;
    private final String fillIn;
    private final Integer maxFeatures;

    private SeparatedStringExtractorConfig(String indexName,
                                           String attrName,
                                           String feaName,
                                           String separator,
                                           boolean normalize,
                                           String fillIn,
                                           Integer maxFeatures) {
        this.indexName = indexName;
        this.attrName = attrName;
        this.feaName = feaName;
        this.separator = separator;
        this.normalize = normalize;
        this.fillIn = fillIn;
        this.maxFeatures = maxFeatures;
    }

    public FeatureExtractor getFeatureExtractor(RequestContext requestContext) {
        return new SeparatedStringExtractor(indexName, attrName, feaName, separator,
                normalize, fillIn, maxFeatures);
    }

    public static FeatureExtractorConfig
            getFeatureExtractorConfig(Configuration extractorConfig,
                                      Injector injector) {
        Boolean normalize = extractorConfig.getBoolean("normalize");
        if (normalize == null) {
            normalize = true;
        }
        return new SeparatedStringExtractorConfig(
                extractorConfig.getString("indexName"),
                extractorConfig.getString("attrName"),
                extractorConfig.getString("feaName"),
                extractorConfig.getString("separator"),
                normalize, extractorConfig.getString("fillIn"),
                extractorConfig.getInt("maxFeatures"));
    }
}
