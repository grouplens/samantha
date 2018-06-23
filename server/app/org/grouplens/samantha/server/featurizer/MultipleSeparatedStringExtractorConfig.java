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

package org.grouplens.samantha.server.featurizer;

import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.featurizer.MultipleSeparatedStringExtractor;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class MultipleSeparatedStringExtractorConfig implements FeatureExtractorConfig {
    private final List<String> indexNames;
    private final List<String> attrNames;
    private final List<String> keyPrefixes;
    private final List<String> feaNames;
    private final String separator;
    private final boolean normalize;
    private final String fillIn;

    private MultipleSeparatedStringExtractorConfig(List<String> indexNames,
                                                   List<String> attrNames,
                                                   List<String> keyPrefixes,
                                                   List<String> feaNames,
                                                   String separator,
                                                   boolean normalize,
                                                   String fillIn) {
        this.indexNames = indexNames;
        this.attrNames = attrNames;
        this.keyPrefixes = keyPrefixes;
        this.feaNames = feaNames;
        this.separator = separator;
        this.normalize = normalize;
        this.fillIn = fillIn;
    }

    public FeatureExtractor getFeatureExtractor(RequestContext requestContext) {
        return new MultipleSeparatedStringExtractor(indexNames, attrNames,
                keyPrefixes, feaNames, separator, normalize, fillIn);
    }

    public static FeatureExtractorConfig
            getFeatureExtractorConfig(Configuration extractorConfig,
                                      Injector injector) {
        Boolean normalize = extractorConfig.getBoolean("normalize");
        if (normalize == null) {
            normalize = true;
        }
        List<String> keyPrefixes = extractorConfig.getStringList("keyPrefixes");
        if (keyPrefixes == null) {
            keyPrefixes = extractorConfig.getStringList("attrNames");
        }
        return new MultipleSeparatedStringExtractorConfig(
                extractorConfig.getStringList("indexNames"),
                extractorConfig.getStringList("attrNames"),
                keyPrefixes,
                extractorConfig.getStringList("feaNames"),
                extractorConfig.getString("separator"),
                normalize, extractorConfig.getString("fillIn"));
    }
}
