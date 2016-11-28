package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FeatureSupportRetriever extends AbstractRetriever {
    final private SVDFeature model;
    final private List<String> itemAttrs;
    final private String supportAttr;
    final private Integer maxHits;
    final private List<EntityExpander> expanders;
    volatile private List<ObjectNode> cached;

    public FeatureSupportRetriever(SVDFeature model, List<String> itemAttrs, String supportAttr,
                                   Integer maxHits, List<EntityExpander> expanders, Configuration config) {
        super(config);
        this.model = model;
        this.itemAttrs = itemAttrs;
        this.supportAttr = supportAttr;
        this.maxHits = maxHits;
        this.expanders = expanders;
    }

    public RetrievedResult retrieve(RequestContext requestContext) {
        List<ObjectNode> entities;
        if (cached == null) {
            Object2DoubleMap<String> fea2sup = model.getFactorFeatures(10);
            List<ObjectNode> all = new ArrayList<>(fea2sup.size());
            for (Object2DoubleMap.Entry<String> entry : fea2sup.object2DoubleEntrySet()) {
                ObjectNode one = Json.newObject();
                one.put(supportAttr, entry.getDoubleValue());
                Map<String, String> keys = FeatureExtractorUtilities.decomposeKey(entry.getKey());
                boolean include = true;
                for (String attr : itemAttrs) {
                    if (!keys.containsKey(attr)) {
                        include = false;
                    }
                }
                if (include) {
                    for (String attr : itemAttrs) {
                        one.put(attr, keys.get(attr));
                    }
                    all.add(one);
                }
            }
            Ordering<ObjectNode> ordering = RetrieverUtilities.jsonFieldOrdering(supportAttr);
            int limit = all.size();
            if (maxHits != null && maxHits < limit) {
                limit = maxHits;
            }
            entities = ordering.greatestOf(all, limit);
            cached = entities;
        } else {
            entities = cached;
        }
        entities = ExpanderUtilities.expand(entities, expanders, requestContext);
        return new RetrievedResult(entities, entities.size());
    }
}
