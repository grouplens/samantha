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
