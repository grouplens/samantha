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
import org.grouplens.samantha.modeler.featurizer.EnglishTokenizeExtractor;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class EnglishTokenizeExtractorConfig implements FeatureExtractorConfig {
    private final String indexName;
    private final String feaName;
    private final List<String> attrNames;
    private final String vocabularyName;
    private final boolean sigmoid;

    private EnglishTokenizeExtractorConfig(String indexName,
                                           List<String> attrNames,
                                           String feaName,
                                           String vocabularyName,
                                           boolean sigmoid) {
        this.indexName = indexName;
        this.attrNames = attrNames;
        this.feaName = feaName;
        this.vocabularyName = vocabularyName;
        this.sigmoid = sigmoid;
    }

    public FeatureExtractor getFeatureExtractor(RequestContext requestContext) {
        return new EnglishTokenizeExtractor(indexName, attrNames, feaName, vocabularyName, sigmoid);
    }

    public static FeatureExtractorConfig
            getFeatureExtractorConfig(Configuration extractorConfig,
                                      Injector injector) {
        Boolean sigmoid = extractorConfig.getBoolean("sigmoid");
        if (sigmoid == null) {
            sigmoid = true;
        }
        return new EnglishTokenizeExtractorConfig(
                extractorConfig.getString("indexName"),
                extractorConfig.getStringList("attrNames"),
                extractorConfig.getString("feaName"),
                extractorConfig.getString("vocabularyName"),
                sigmoid
        );
    }
}
