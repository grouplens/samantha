package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.Logger;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class ColumnToRowExpander implements EntityExpander {
    final private List<String> colNames;
    final private String nameAttr;
    final private String valueAttr;

    private ColumnToRowExpander(String nameAttr, String valueAttr, List<String> colNames) {
        this.colNames = colNames;
        this.nameAttr = nameAttr;
        this.valueAttr = valueAttr;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        return new ColumnToRowExpander(expanderConfig.getString("nameAttr"),
            expanderConfig.getString("valueAttr"), 
            expanderConfig.getStringList("colNames"));
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        List<ObjectNode> expanded = new ArrayList<>();
        for (ObjectNode entity : initialResult) {
            List<ObjectNode> oneExpanded = new ArrayList<>();
            for (String colName : colNames) {
                if (entity.has(colName)) {
                    ObjectNode newEntity = entity.deepCopy();
                    newEntity.put(nameAttr, colName);
                    newEntity.set(valueAttr, entity.get(colName));
                    oneExpanded.add(newEntity);
                } else {
                    Logger.warn("The column {} is not present: {}", colName, entity.toString());
                }
            }
            if (oneExpanded.size() > 0) {
                expanded.addAll(oneExpanded);
            } else {
                expanded.add(entity);
            }
        }
        return expanded;
    }
}
