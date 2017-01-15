package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class FieldThresholdFilterExpander implements EntityExpander {
    private final String filterAttr;
    private final Double minVal;
    private final Double maxVal;
    private final boolean filterWhenNotPresent;

    public FieldThresholdFilterExpander(String filterAttr, Double minVal, Double maxVal,
                                        boolean filterWhenNotPresent) {
        this.filterAttr = filterAttr;
        this.minVal = minVal;
        this.maxVal = maxVal;
        this.filterWhenNotPresent = filterWhenNotPresent;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        Boolean filterWhenNotPresent = expanderConfig.getBoolean("filterWhenNotPresent");
        if (filterWhenNotPresent == null) {
            filterWhenNotPresent = true;
        }
        return new FieldThresholdFilterExpander(expanderConfig.getString("filterAttr"),
                expanderConfig.getDouble("minVal"),
                expanderConfig.getDouble("maxVal"),
                filterWhenNotPresent);
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext){
        List<ObjectNode> filteredResult = new ArrayList<>();
        for (int i=0; i<initialResult.size(); i++) {
            ObjectNode entity = initialResult.get(i);
            if (entity.has(filterAttr)) {
                double val = entity.get(filterAttr).asDouble();
                if ((minVal != null && val < minVal) || (maxVal != null && val > maxVal)) {
                    continue;
                }
            } else {
                if (filterWhenNotPresent) {
                    continue;
                }
            }
            filteredResult.add(entity);
        }
        return filteredResult;
    }
}
