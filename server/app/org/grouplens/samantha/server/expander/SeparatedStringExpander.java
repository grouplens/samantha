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
import play.inject.Injector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeparatedStringExpander implements EntityExpander {
    final private List<String> nameAttrs;
    final private List<String> valueAttrs;
    final private String separator;
    final private String withDefault;

    public SeparatedStringExpander(List<String> nameAttrs, List<String> valueAttrs, String separator,
                                    String withDefault) {
        this.nameAttrs = nameAttrs;
        this.valueAttrs = valueAttrs;
        this.separator = separator;
        this.withDefault = withDefault;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        return new SeparatedStringExpander(
                expanderConfig.getStringList("nameAttrs"),
                expanderConfig.getStringList("valueAttrs"),
                expanderConfig.getString("separator"),
                expanderConfig.getString("withDefault"));
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        List<ObjectNode> expanded = new ArrayList<>();
        for (ObjectNode entity : initialResult) {
            Map<String, String[]> name2values = new HashMap<>();
            for (String nameAttr : nameAttrs) {
                if (entity.has(nameAttr)) {
                    name2values.put(nameAttr, entity.get(nameAttr).asText().split(separator, -1));
                }
            }
            List<ObjectNode> oneExpanded = new ArrayList<>();
            if (name2values.size() > 0) {
                int size = name2values.values().iterator().next().length;
                for (int i = 0; i < size; i++) {
                    ObjectNode newEntity = entity.deepCopy();
                    for (int j=0; j<valueAttrs.size(); j++) {
                        if (name2values.containsKey(nameAttrs.get(j))) {
                            newEntity.put(valueAttrs.get(j), name2values.get(nameAttrs.get(j))[i]);
                        } else if (withDefault != null) {
                            newEntity.put(valueAttrs.get(j), withDefault);
                        }
                    }
                    oneExpanded.add(newEntity);
                }
            }
            if (oneExpanded.size() > 0) {
                expanded.addAll(oneExpanded);
            } else if (withDefault != null) {
                for (String valueAttr : valueAttrs) {
                    entity.put(valueAttr, withDefault);
                }
                expanded.add(entity);
            }
        }
        return expanded;
    }
}
