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

package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

public class StepToSequenceExpander implements EntityExpander {
    final private List<String> nameAttrs;
    final private List<String> valueAttrs;
    final private List<String> otherAttrs;
    final private String joiner;

    public StepToSequenceExpander(List<String> nameAttrs, List<String> valueAttrs,
                                  List<String> otherAttrs, String joiner) {
        this.nameAttrs = nameAttrs;
        this.valueAttrs = valueAttrs;
        this.otherAttrs = otherAttrs;
        this.joiner = joiner;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        return new StepToSequenceExpander(
                expanderConfig.getStringList("nameAttrs"),
                expanderConfig.getStringList("valueAttrs"),
                expanderConfig.getStringList("otherAttrs"),
                expanderConfig.getString("joiner"));
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        if (initialResult.size() == 0) {
            return initialResult;
        }
        List<List<String>> values = new ArrayList<>();
        for (int i=0; i<nameAttrs.size(); i++) {
            values.add(new ArrayList<>());
        }
        for (ObjectNode entity : initialResult) {
            for (int i=0; i<nameAttrs.size(); i++) {
                String val = "";
                if (entity.has(nameAttrs.get(i))) {
                    val = entity.get(nameAttrs.get(i)).asText();
                }
                values.get(i).add(val);
            }
        }
        ObjectNode entity;
        if (otherAttrs != null) {
            entity = Json.newObject();
            for (String attr : otherAttrs) {
                entity.set(attr, initialResult.get(0).get(attr));
            }
        } else {
            entity = initialResult.get(0).deepCopy();
        }
        for (int i=0; i<nameAttrs.size(); i++) {
            entity.put(valueAttrs.get(i), StringUtils.join(values.get(i), joiner));
        }
        List<ObjectNode> expanded = new ArrayList<>();
        expanded.add(entity);
        return expanded;
    }
}
