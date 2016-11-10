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

    public FieldThresholdFilterExpander(String filterAttr, Double minVal, Double maxVal) {
        this.filterAttr = filterAttr;
        this.minVal = minVal;
        this.maxVal = maxVal;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        return new FieldThresholdFilterExpander(expanderConfig.getString("filterAttr"),
                expanderConfig.getDouble("minVal"),
                expanderConfig.getDouble("maxVal"));
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext){
        List<ObjectNode> filteredResult = new ArrayList<>();
        for (int i=0; i<initialResult.size(); i++) {
            ObjectNode entity = initialResult.get(i);
            double val = 0.0;
            if (entity.has(filterAttr)) {
                val = entity.get(filterAttr).asDouble();
            }
            if ((minVal != null && val < minVal) || (maxVal != null && val > maxVal)) {
                continue;
            }
            filteredResult.add(entity);
        }
        return filteredResult;
    }
}
