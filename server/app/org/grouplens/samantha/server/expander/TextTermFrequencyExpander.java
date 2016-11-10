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
            }
        }
        return expandedList;
    }
}
