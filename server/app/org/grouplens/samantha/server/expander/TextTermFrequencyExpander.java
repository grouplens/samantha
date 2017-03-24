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
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.modeler.featurizer.SelfPlusOneRatioFunction;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.Logger;
import play.inject.Injector;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TextTermFrequencyExpander implements EntityExpander {
    final private String termField;
    final private String textField;
    final private String normTermFreqField;
    final private Analyzer analyzer;
    final private UnivariateFunction function;

    public TextTermFrequencyExpander(String termField, String textField, String normTermFreqField) {
        this.normTermFreqField = normTermFreqField;
        this.textField = textField;
        this.termField = termField;
        this.analyzer = new EnglishAnalyzer();
        this.function = new SelfPlusOneRatioFunction();
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        return new TextTermFrequencyExpander(expanderConfig.getString("termField"),
                expanderConfig.getString("textField"),
                expanderConfig.getString("normTermFreqField"));
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                  RequestContext requestContext) {
        List<ObjectNode> expandedList = new ArrayList<>();
        for (ObjectNode entity : initialResult) {
            if (entity.has(textField)) {
                String text = entity.get(textField).asText();
                Map<String, Integer> termFreq = FeatureExtractorUtilities.getTermFreq(analyzer, text, termField);
                for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
                    ObjectNode newEntity = Json.newObject();
                    IOUtilities.parseEntityFromJsonNode(entity, newEntity);
                    newEntity.put(termField, entry.getKey());
                    newEntity.put(normTermFreqField, function.value(entry.getValue()));
                    expandedList.add(newEntity);
                }
            } else {
                Logger.warn("The text field is not present: {}", entity.toString());
            }
        }
        return expandedList;
    }
}
